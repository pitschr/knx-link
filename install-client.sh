#!/bin/bash

##
## Minimum Requirement Check
##
echo "********************************************************************"
echo "                    CHECKING MINIMUM REQUIREMENTS                   "
echo "                                                                    "
echo " 1) 'curl' to download the latest file                              "
echo "********************************************************************"

# Check if 'curl' is available, this is required to download the JAR file from GitHub
if [[ -z "$(type -p curl)" ]]; then
  echo "[ERROR] 'curl' not installed? Please install 'curl'"
  exit 1
fi

# Select the latest tag name and then get the download URL
KNX_LINK_LATEST_TAG_NAME=$(curl --header 'Accept: application/vnd.github.v3+json' -s https://api.github.com/repos/pitschr/knx-link/tags | fgrep "knx-link-client-" | fgrep "\"name\":" | cut -d\" -f4 | sort -nr | head -n1)
if [[ ! $KNX_LINK_LATEST_TAG_NAME =~ ^knx-link-client-[0-9\.]+[a-z]?$ ]]; then
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
## Check Operation System
##
OS_NAME=""
case "$(uname -s)" in
    Linux*)     OS_NAME=LINUX;;
    Darwin*)    OS_NAME=MAC;;
    CYGWIN*)    OS_NAME=CYGWIN;;
    MINGW*)     OS_NAME=MINGW;;
    *)
        echo "Could not detect the operation system. Exit. Output: $(uname)"
        exit 1
esac

DOWNLOAD_URL="https://github.com/pitschr/knx-link/releases/download/${KNX_LINK_LATEST_TAG_NAME}"
# Detecting OS
if [ "$OS_NAME" = "MAC" ]; then
  DOWNLOAD_URL="$DOWNLOAD_URL/knx-link-client-macos"
  echo "It looks like you are using MacOS. Downloading file: ${DOWNLOAD_URL}"
elif [ "$OS_NAME" = "LINUX" ]; then
  if [[ -z "$(type -p ldd)" ]]; then
    DOWNLOAD_URL="$DOWNLOAD_URL/knx-link-client-linux-musl"
    echo "It looks like you are using Linux but no GLIBC is installed (command: ldd). Downloading file: ${DOWNLOAD_URL}"
  else
    LDD_VERSION=$(ldd --version | head -n1 | sed 's/.* \([0-9]\+\)\.\([0-9]\+\)$/\1\2/g')
    if [[ $LDD_VERSION -ge 214 ]]; then
      DOWNLOAD_URL="$DOWNLOAD_URL/knx-link-client-linux-gnu"
      echo "It looks like you are using Linux with a compatible GLIBC. Downloading file: ${DOWNLOAD_URL}"
    else
      DOWNLOAD_URL="$DOWNLOAD_URL/knx-link-client-linux-musl"
      echo "It looks like you are using Linux with an outdated GLIBC. Downloading file: ${DOWNLOAD_URL}"
    fi
  fi
elif [ "$OS_NAME" = "CYGWIN" ]; then
  DOWNLOAD_URL="$DOWNLOAD_URL/knx-link-client-linux-musl"
  echo "It looks like you are using Cygwin. Downloading file: ${DOWNLOAD_URL}"
elif [ "$OS_NAME" = "MINGW" ]; then
  DOWNLOAD_URL="$DOWNLOAD_URL/knx-link-client-linux-musl"
  echo "It looks like you are using MinGw. Downloading file: ${DOWNLOAD_URL}"
else
  echo "Huh? How did you entered here?"
  exit 2
fi
curl --location -s -o "knx-link-client" "$DOWNLOAD_URL"
chmod +x "knx-link-client"
echo
echo "INSTALLATION COMPLETED!"
echo