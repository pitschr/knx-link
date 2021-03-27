#!/bin/bash

##
## Minimum Requirement Check
##
echo "********************************************************************"
echo "                    CHECKING MINIMUM REQUIREMENTS                   "
echo "                                                                    "
echo " 1) 'java' for KNX service and must be Java 11 and above            "
echo " 2) 'curl' to download the latest file                              "
echo " 3) 'systemctl' to run KNX as a daemon service                      "
echo "********************************************************************"

javaCmd=$(type -p java)
# Check if 'java' is available
if [[ -z "$javaCmd" ]]; then
	echo "[ERROR] Java not installed? Please install Java 11 or above."
	exit 1
else
  # Check if 'java' is version 11 or above
  javaCmdVersion=$(javap -verbose java.lang.Void | fgrep "major version" | cut -d':' -f2 | xargs)
  if [[ $javaCmdVersion -lt 55 ]]; then
    echo "[ERROR] You have Java major version '$javaCmdVersion', required is Java 11 (major version 55 and above)."
    exit 1
  fi
fi

# Check if 'curl' is available, this is required to download the JAR file from GitHub
curlCmd=$(type -p curl)
if [[ -z "$curlCmd" ]]; then
  echo "[ERROR] Curl not installed? Please install 'curl'"
  exit 1
fi

# Check if 'systemctl' is available, this is required to install the service
systemctlCmd=$(type -p systemctl)
if [[ -z "$systemctlCmd" ]]; then
  echo "[ERROR] Systemd not installed? Probably your linux version is outdated or running in a container?"
  exit 1
fi

# Figure out the latest file
DOWNLOAD_FILE="$(curl -s https://api.github.com/repos/pitschr/knx-core/releases/latest | fgrep "tag_name" | cut -d\" -f4).jar"
if [[ -z "$DOWNLOAD_FILE" ]]; then
  echo "[ERROR] Could not fetch the latest file from GitHub. Please contact the maintainer."
  exit 2
fi

echo
echo "Minimum requirement passed!"
echo
echo "********************************************************************"
echo "                            INSTALLATION                            "
echo "********************************************************************"

##
## Install user 'knx' for systemd service
##
echo -n "Add user 'knx' ... "
if ! id "knx" &> /dev/null; then
  useradd --no-create-home --system knx
  if ! id "knx" &> /dev/null; then
		echo "[ERROR] Something went wrong during adding user and group 'knx'."
		exit 3
	fi
	echo "DONE"
else
  echo "DONE (already installed)"
fi

##
## Download 'jar' file from GitHub
##
echo -n "Download file '$DOWNLOAD_FILE' ... "
curl -s -o "/usr/local/bin/knx-link-server.jar" "https://github.com/pitschr/knx-core/archive/refs/tags/$DOWNLOAD_FILE"
echo "DONE"

##
## Install 'knx' system service
##
SYSTEMD_SERVICE_KNX=/etc/systemd/system/knx.service
if [[ -f $SYSTEMD_SERVICE_KNX ]]; then
	read -p "Systemd 'knx.service' found. Overwrite now? [Yn]: " yn
	if [[ $yn != [Yy]* ]]; then
		echo "You said '$yn'. Aborted."
		exit 4
	fi

	echo -n "Stopping systemd 'knx.service' ... "
	systemctl stop knx.service
	echo "DONE"
fi

echo -n "Installing systemd 'knx.service' ... "
cat << 'EOF' > "$SYSTEMD_SERVICE_KNX"
[Unit]
Description=KNX Service
After=network.target

[Service]
Type=simple
Restart=always
RestartSec=3
User=knx
WorkingDirectory=/usr/local/bin
ExecStart=java -version

[Install]
WantedBy=multi-user.target
EOF
echo "DONE"

##
## Setup Firewall (optional)
##
firewallCmd=$(type -p "firewall-cmd")
if [[ ! -z "$firewallCmd" ]]; then
  firewall_knx_service=$(firewall-cmd --info-service=knx 2> /dev/null)
  if [[ -z "$firewall_knx_service" ]]; then
    echo "********************************************************************"
    echo " A T T E N T I O N"
    echo
    echo " On your machine I found 'firewall-cmd'"
    echo
    echo " Next steps is to setup a new firewall service called 'knx' which"
    echo " opens the official KNX registered UDP port: 3671"
    echo
    echo " At the end the firewall daemon will be reloaded to make the new"
    echo " service effective."
    echo "********************************************************************"
    read -p "Shall I proceed with creating a new firewall service? [Yn]: " yn
    if [[ $yn == [Yy]* ]]; then
      echo -n "Creating firewall service 'knx' ... "
      firewall-cmd -q --permanent --new-service=knx
      firewall-cmd -q --permanent --service=knx --set-description="KNXnet/IP is a part of KNX standard for transmission of KNX telegrams via Ethernet"
      firewall-cmd -q --permanent --service=knx --set-short=KNX
      firewall-cmd -q --permanent --service=knx --add-port=3671/udp
      echo "DONE"

      echo -n "Registering firewall service 'knx' to current zone ... "
      firewall-cmd -q --permanent --add-service=knx
      echo "DONE"

      echo -n "Firewall service will be reloaded ... "
      firewall-cmd -q --reload
      echo "DONE"
    else
      echo "You said '$yn'. Create firewall service 'knx' will be skipped."
    fi
  else
    echo "********************************************************************"
    echo " A T T E N T I O N"
    echo
    echo " On your machine I found 'firewall-cmd' and service 'knx'"
    echo
    echo " Next steps is to ensure that your firewall configuration is"
    echo " up-to-date and opens the official KNX registered UDP port: 3671"
    echo
    echo " At the end the firewall daemon will be reloaded to make the new"
    echo " service effective."
    echo "********************************************************************"
    read -p "Shall I proceed with updating the existing firewall service? [Yn]: " yn
    if [[ $yn == [Yy]* ]]; then
      echo -n "Updating firewall service 'knx' ... "
      firewall-cmd -q --permanent --service=knx --add-port=3671/udp
      echo "DONE"

      echo -n "Registering firewall service 'knx' to current zone ... "
      firewall-cmd -q --permanent --add-service=knx
      echo "DONE"

      echo -n "Firewall service will be reloaded ... "
      firewall-cmd -q --reload
      echo "DONE"
    else
      echo "You said '$yn'. Updating firewall service 'knx' will be skipped."
    fi
  fi
else
  echo "[WARN] No 'firewall-cmd' found. If you are using a different firewall service, please ensure that port 3671 is open."
fi

##
## Reload and Start 'knx' system service
##
echo -n "Starting systemd 'knx.service' ... "
systemctl daemon-reload
systemctl enable knx.service
systemctl start knx.service
echo "DONE"

echo -n "Checking systemd 'knx.service' ... "
sleep 1
SYSTEMD_STATUS=$(systemctl status knx.service)
if [[ $(echo "$SYSTEMD_STATUS" | fgrep "Active: active" | wc -l) -eq 0 ]]; then
  echo "[ERROR] Something went wrong? The status should be 'active', but was not. Please check:"
  echo "$SYSTEMD_STATUS"
  exit 5
fi
echo "DONE"

CHECK_PORT=10222
echo -n "Checking port for 'knx.service' ... "
if ! ncat -vz localhost $CHECK_PORT &> /dev/null; then
  echo "[ERROR] Port seems not be open? Please check if knx daemon is listening on port: $CHECK_PORT"
  exit 6
fi
echo "DONE"
echo
echo "********************************************************************"
echo "                        INSTALLATION COMPLETED"
echo "********************************************************************"
echo
