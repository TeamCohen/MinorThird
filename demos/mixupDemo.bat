@echo off
echo + This demo seems to be broken now, try another one.
goto end
echo + This demo displays the results of a few versions of a mixup file.
echo + It uses the class MixupDebugger to evaluate and display results.
echo + 
echo + This demo uses sampleData/airtimeExample.txt for data.
echo +
@echo on

java edu.cmu.minorthird.text.gui.MixupDebugger -file sampleData\airtimeExample.txt -mixup sampleMixup\timev-1.mixup

java edu.cmu.minorthird.text.gui.MixupDebugger -file sampleData\airtimeExample.txt -mixup sampleMixup\timev-2.mixup

java edu.cmu.minorthird.text.gui.MixupDebugger -file sampleData\airtimeExample.txt -mixup sampleMixup\time.mixup

:end
