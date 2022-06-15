#!/bin/bash

KNX_LINK_SERVER_PORT=3672

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

# Check if 'java' is available
if [[ -z "$(type -p java)" ]]; then
  echo "[ERROR] Java not installed? Please install Java 11 or above."
  exit 1
else
  # Check if 'java' is version 11 or above
  # 1.8 will be interpreted as 18
  # 11.0 will be interpreted as 110
  javaVersion=$(java -version 2>&1 | sed 's/.*"\([^\.]*\)\.\([^\.]*\).*".*/\1\2/; 1q')
  if [[ $javaVersion -lt 110 ]]; then
    echo "[ERROR] Old Java version? Please install Java 11 or above."
    exit 1
  fi
fi

# Check if 'curl' is available, this is required to download the JAR file from GitHub
if [[ -z "$(type -p curl)" ]]; then
  echo "[ERROR] 'curl' not installed? Please install 'curl'"
  exit 1
fi

# Check if 'systemctl' is available, this is required to install the service
if [[ -z "$(type -p systemctl)" ]]; then
  echo "[ERROR] 'systemctl' not installed? Probably your linux version is outdated or running in a container?"
  exit 1
fi

# Select the latest tag name and then get the download URL
KNX_LINK_LATEST_TAG_NAME=$(curl --header 'Accept: application/vnd.github.v3+json' -s https://api.github.com/repos/pitschr/knx-link/tags | fgrep "knx-link-server-" | fgrep "\"name\":" | cut -d\" -f4)
if [[ ! $KNX_LINK_LATEST_TAG_NAME =~ ^knx-link-server-[0-9\.]+$ ]]; then
  echo "[ERROR] Latest Tag Name Response: '$KNX_LINK_LATEST_TAG_NAME'"
  echo "[ERROR] Could not fetch the latest tag name from GitHub. Please contact the maintainer."
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
KNX_LINK_FOLDER="/opt/$KNX_LINK_LATEST_TAG_NAME"
KNX_LINK_SERVER_JAR="$KNX_LINK_FOLDER/server.jar"
echo -n "Download file '${KNX_LINK_LATEST_TAG_NAME}.jar' to '$KNX_LINK_SERVER_JAR' ... "
mkdir "$KNX_LINK_FOLDER"
if [[ ! -d "$KNX_LINK_FOLDER" ]]; then
  echo "[ERROR] Could not create folder '$KNX_LINK_FOLDER'. Please check your permission."
  exit 5
fi 
DOWNLOAD_URL="https://github.com/pitschr/knx-link/releases/download/${KNX_LINK_LATEST_TAG_NAME}/${KNX_LINK_LATEST_TAG_NAME}.jar"
curl --location -s -o "$KNX_LINK_SERVER_JAR" "$DOWNLOAD_URL"
if [[ ! -f "$KNX_LINK_SERVER_JAR" ]]; then
  echo "[ERROR] File could not be downloaded from '$DOWNLOAD_URL' to '$KNX_LINK_SERVER_JAR'. Please contact the maintainer."
  exit 5
fi
chown -R knx:knx "$KNX_LINK_FOLDER"
if [[ $? -ne 0 ]]; then
  echo "[ERROR] Could not change the ownership of folder '$KNX_LINK_FOLDER' for user 'knx'."
  exit 5
fi
echo "DONE"

##
## Install 'knx' system service
##
SYSTEMD_SERVICE_KNX=/etc/systemd/system/knx.service
if [[ -f "$SYSTEMD_SERVICE_KNX" ]]; then
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
cat << EOF > "$SYSTEMD_SERVICE_KNX"
[Unit]
Description=KNX Link Service
After=network.target

[Service]
Type=simple
Restart=always
RestartSec=3
User=knx
WorkingDirectory=$KNX_LINK_FOLDER
ExecStart=java -jar "$KNX_LINK_SERVER_JAR"

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
    echo " opens the official KNX registered UDP port: 3671. For communication"
    echo " without NAT, the UDP ports are opened too: 40001 (for Description),"
    echo " 40002 (for Control) and 40003 (for Data)"
    echo
    echo " At the end the firewall daemon will be reloaded to make the new"
    echo " service effective."
    echo
    echo " If you want to skip the creating new firewall, then choose 'no'."
    echo "********************************************************************"
    read -p "Shall I proceed with creating a new firewall service? [Yn]: " yn
    if [[ $yn == [Yy]* ]]; then
      echo -n "Creating firewall service 'knx' ... "
      firewall-cmd -q --permanent --new-service=knx
      firewall-cmd -q --permanent --service=knx --set-description="KNXnet/IP is a part of KNX standard for transmission of KNX telegrams via Ethernet"
      firewall-cmd -q --permanent --service=knx --set-short=KNX
      firewall-cmd -q --permanent --service=knx --add-port=3671/udp
      firewall-cmd -q --permanent --service=knx --add-port=40001-40003/udp
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
    echo " For communication without NAT, the UDP ports are configured to open:"
    echo " 40001 (for Description), 40002 (for Control) and 40003 (for Data)"
    echo
    echo " At the end the firewall daemon will be reloaded to make the new"
    echo " service effective."
    echo
    echo " If you want to skip the updating the firewall, then choose 'no'."
    echo "********************************************************************"
    read -p "Shall I proceed with updating the existing firewall service? [Yn]: " yn
    if [[ $yn == [Yy]* ]]; then
      echo -n "Updating firewall service 'knx' ... "
      firewall-cmd -q --permanent --service=knx --add-port=3671/udp
      firewall-cmd -q --permanent --service=knx --add-port=40001-40003/udp
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
sleep 5
SYSTEMD_STATUS=$(systemctl status knx.service)
if [[ $(echo "$SYSTEMD_STATUS" | fgrep "Active: active" | wc -l) -eq 0 ]]; then
  echo "[ERROR] Something went wrong? The status should be 'active', but was not. Please check:"
  echo "$SYSTEMD_STATUS"
  exit 5
fi
echo "DONE"
echo
echo "INSTALLATION COMPLETED!"
echo
echo "********************************************************************"
echo "                      POST INSTALLATION CHECKS                      "
echo "********************************************************************"
echo -n "Checking port for 'knx.service' ... "
ncatCmd=$(type -p ncat)
ncCmd=$(type -p nc)
if [[ -n "$ncatCmd" || -n "$ncCmd" ]]; then  # 'ncat' is available
  PORT_CHECK=1
  if [[ -n "$ncatCmd" ]]; then
    PORT_CHECK=$(ncat -z "localhost" $KNX_LINK_SERVER_PORT; echo $?)
  else
    PORT_CHECK=$(nc "localhost" $KNX_LINK_SERVER_PORT </dev/null > /dev/null 2>&1; echo $?)
  fi
  if [[ $PORT_CHECK -ne 0 ]]; then
    echo "[ERROR] Port seems not be open? Please check if knx daemon is listening on port: $KNX_LINK_SERVER_PORT"
    exit 6
  fi
  echo "DONE"
else
  echo "SKIPPED (no 'ncat' nor 'nc' command found)"
fi
