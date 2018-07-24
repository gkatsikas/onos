#!/bin/bash

download_and_install_influxdb()
{
	FILENAME="influxdb_1.5.0_amd64.deb"
	echo "Downloading $FILENAME ... For more info, visit https://portal.influxdata.com/downloads"

	if [ -f $HOME/$FILENAME ]; then
		echo "File $HOME/$FILENAME exists. NOT downloading again..."
	else
		echo "Downloading $FILENAME in directory $HOME ..."
		wget https://dl.influxdata.com/influxdb/releases/$FILENAME -P $HOME
	fi
	echo "Installing package $FILENAME ..."
	sudo dpkg -i $HOME/$FILENAME

	rm -f $HOME/$FILENAME
}

install_tcpreplay()
{
	sudo apt install -y tcpreplay
}

download_and_install_influxdb
install_tcpreplay
