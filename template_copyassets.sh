#!/bin/bash

S=~/dev/apps/ide/jandroid

A=assets

# CHANGE THE FOLLOW TO YOUR OWN FOLDERS
P=~/j903/addons
J=~/j903

cd $S

rm -rf $S/$A
mkdir -p $S/$A
cp -r $J/system $S/$A/.

find $A \( -name -name '*.dll' -o -name '*.exe' -o -name '*.so' -o -name '*.dylib' -o -name '*.sh' -o -name '*.bat' '*.cmd' -o -name '.*' \) -delete

mkdir -p $S/$A/bin
echo "j903 install" > $S/$A/bin/installer.txt

copyaddon() {
rm -rf $S/$A/addons/$1/$2
mkdir -p $S/$A/addons/$1
cp -r $P/$1/$2 $S/$A/addons/$1
}

# copyaddon api expat
# copyaddon api gles
copyaddon api jni
# copyaddon api sl4a
# copyaddon arc zlib
# copyaddon convert json
# copyaddon data jmf
# copyaddon demos isigraph
# copyaddon demos wd
# copyaddon demos wdplot
# copyaddon docs help
# copyaddon games minesweeper
# copyaddon games nurikabe
# copyaddon games pousse
# copyaddon games solitaire
# copyaddon general misc
# copyaddon graphics afm
# copyaddon graphics bmp
# copyaddon graphics color
# copyaddon graphics gl2
# copyaddon graphics graph
# copyaddon graphics grid
# copyaddon graphics plot
# copyaddon graphics png
# copyaddon graphics print
# copyaddon graphics viewmat
copyaddon ide ja
copyaddon ide jhs
# copyaddon ide qt
# copyaddon labs labs
# copyaddon math deoptim
# copyaddon math misc
# copyaddon stats base

find $A/addons \( -name '*.jproj' -o -name '*.dll' -o -name '*.exe' -o -name '*.so' -o -name '*.dylib' -o -name 'd3.v3.min.js' -o -name 'baselibtags' -o -name '.*' \) -delete

cp $S/assets_version.txt $S/$A/.
