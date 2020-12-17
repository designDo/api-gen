package com.sia.apigen;

import com.siyeh.ig.ui.UiUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.event.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JFormattedTextField path_field;
    private JFormattedTextField method_field;

    public AndroidDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        /*String s = " @ Field()   String a   ";
        String realS = s.trim();
        System.out.println(realS);

        String argument = realS.substring(realS.lastIndexOf(" ") + 1);
        System.out.println(argument);

        String first = realS.substring(0, realS.lastIndexOf(" ")).replaceAll(" ", "");
        System.out.println(first);

        String annotation = first.substring(first.indexOf("@") + 1, first.indexOf("("));
        System.out.println(annotation);

        if (first.indexOf(")") - first.indexOf("(") ==1) {
            System.out.println("null annotationValue");
        }else {
            String annotationValue = first.substring(first.indexOf("(") + 2, first.indexOf(")") - 1);
            System.out.println(annotationValue);
        }
        String type = first.substring(first.indexOf(")")+1);
        System.out.println(type);*/
        String s = "";

        String a = "id";

        System.out.println(s.replace("{id}","$id"));
        System.out.println(s.substring(s.lastIndexOf("/") +1));
    }
}
