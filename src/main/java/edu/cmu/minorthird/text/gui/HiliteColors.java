package edu.cmu.minorthird.text.gui;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;

/** Some colors for highlighting.
 *
 * @author William Cohen
 */

/*package*/

class HiliteColors
{
    public static final SimpleAttributeSet blue,red,green,yellow,gray,cursorColor;

    static
    {
        gray = new SimpleAttributeSet();
        StyleConstants.setForeground(gray, Color.darkGray);
        StyleConstants.setBackground(gray, Color.lightGray);
        yellow = new SimpleAttributeSet();
        StyleConstants.setForeground(yellow, Color.black);
        StyleConstants.setBackground(yellow, Color.yellow);
        blue = new SimpleAttributeSet();
        StyleConstants.setForeground(blue, Color.black);
        StyleConstants.setBackground(blue, Color.cyan);
        green = new SimpleAttributeSet();
        StyleConstants.setForeground(green, Color.black);
        StyleConstants.setBackground(green, Color.green);
        red = new SimpleAttributeSet();
        StyleConstants.setForeground(red, Color.yellow);
        StyleConstants.setBackground(red, Color.red);
        cursorColor = new SimpleAttributeSet();
        StyleConstants.setForeground(cursorColor, Color.yellow);
        StyleConstants.setBackground(cursorColor, Color.blue);
    }
}
