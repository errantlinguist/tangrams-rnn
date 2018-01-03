package org.jtikz;

import org.jtikz.TikzGraphics2D;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Example extends JPanel  {
    public Example() {
        setPreferredSize(new Dimension(80,40));
    }

    public void paintComponent(Graphics g) {
        g.drawOval(3, 10, 5, 30);
        g.setColor(Color.RED);
        g.drawLine(1, 2, 3, 4);
        g.setColor(Color.BLUE);
        g.drawString("This is a test!",3, 10);
        g.setColor(new Color(128, 128, 255, 128));
        g.fillArc(5, 11, 30, 30, 120, 90);
    }

    static JMenuBar getMenu() {
        JMenuBar menuBar;
        JMenu menu, submenu;
        JMenuItem menuItem;
        JRadioButtonMenuItem rbMenuItem;
        JCheckBoxMenuItem cbMenuItem;

        //Create the menu bar.
        menuBar = new JMenuBar();
        
        //Build the first menu.
        menu = new JMenu("A Menu");
        menu.setMnemonic(KeyEvent.VK_A);
        menu.getAccessibleContext().setAccessibleDescription(
                                                             "The only menu in this program that has menu items");
        menuBar.add(menu);

        //a group of JMenuItems
        menuItem = new JMenuItem("A text-only menu item",
                                 KeyEvent.VK_T);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                                                       KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription(
                                                                 "This doesn't really do anything");
        menu.add(menuItem);

        menuItem = new JMenuItem("Both text and icon");
        menuItem.setMnemonic(KeyEvent.VK_B);
        menu.add(menuItem);
        
        menuItem = new JMenuItem();
        menuItem.setMnemonic(KeyEvent.VK_D);
        menu.add(menuItem);

        //a group of radio button menu items
        menu.addSeparator();
        ButtonGroup group = new ButtonGroup();
        rbMenuItem = new JRadioButtonMenuItem("A radio button menu item");
        rbMenuItem.setSelected(true);
        rbMenuItem.setMnemonic(KeyEvent.VK_R);
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem("Another one");
        rbMenuItem.setMnemonic(KeyEvent.VK_O);
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        //a group of check box menu items
        menu.addSeparator();
        cbMenuItem = new JCheckBoxMenuItem("A check box menu item");
        cbMenuItem.setMnemonic(KeyEvent.VK_C);
        menu.add(cbMenuItem);

        cbMenuItem = new JCheckBoxMenuItem("Another one");
        cbMenuItem.setMnemonic(KeyEvent.VK_H);
        menu.add(cbMenuItem);

        //a submenu
        menu.addSeparator();
        submenu = new JMenu("A submenu");
        submenu.setMnemonic(KeyEvent.VK_S);

        menuItem = new JMenuItem("An item in the submenu");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                                                       KeyEvent.VK_2, ActionEvent.ALT_MASK));
        submenu.add(menuItem);

        menuItem = new JMenuItem("Another item");
        submenu.add(menuItem);
        menu.add(submenu);

        //Build second menu in the menu bar.
        menu = new JMenu("Another Menu");
        menu.setMnemonic(KeyEvent.VK_N);
        menu.getAccessibleContext().setAccessibleDescription(
                                                             "This menu does nothing");
        menuBar.add(menu);

        return menuBar;
    }

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("Test!");
        //frame.setJMenuBar(getMenu());
        JPanel p = new Example();
        JPanel c = new JPanel();
        c.add(p);
        c.add(new JButton("Test button!"));
        c.add(new JLabel("Test label!"));
        String[] data = {"one", "two", "three", "four"};
        //JScrollPane jsp = new JScrollPane(new JList(data));
        //jsp.setPreferredSize(new Dimension(60, 60));
        //c.add(jsp);
        c.add(new JList(data));
        String[] petStrings = { "Bird", "Cat", "Dog", "Rabbit", "Pig" };
        JComboBox petList = new JComboBox(petStrings);
        petList.setSelectedIndex(4);
        c.add(petList);
        frame.getContentPane().add(c);
        frame.getContentPane().setPreferredSize(new Dimension(640, 480));
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        TikzGraphics2D t = new TikzGraphics2D();
        t.paintComponent(frame);
    }
}