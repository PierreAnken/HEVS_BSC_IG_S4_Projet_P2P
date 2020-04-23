package p2p_common;

import java.awt.Frame;

import javax.swing.JOptionPane;

public class FatalError {
	
	public FatalError(String error){
		if(!error.equals(""))
			JOptionPane.showMessageDialog(new Frame(),"\n"+error, "Fatal Error", JOptionPane.ERROR_MESSAGE);
		
		System.exit(1);
	}
}
