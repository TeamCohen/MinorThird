echo ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
echo CYGWIN setup script for MontyTagger
echo Run this after the main KLINGER setup script has been run
echo ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
echo
export MONTYLINGUA="${KLINGER-.}/plugins/montytagger"
export CLASSPATH="${CLASSPATH};${MONTYLINGUA}/montylingua.jar"
