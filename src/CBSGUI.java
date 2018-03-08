/**
 * This class sets up the GUI for the SITA
 * check-in counter billing application
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

public class CBSGUI extends JFrame implements ActionListener{
	private JButton chooseDataButton, chooseScheduleButton, runButton;
	private JTextField dataTextField, scheduleTextField;
	private String chosenDataFile = "";
	private String chosenScheduleFile = "";
	private Controller parser;
	
	
	public CBSGUI()
	{
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("Select Files");
		setLocation(200,200);
		setSize(600, 170);
		setLocationRelativeTo(null);		
		layoutPanels();	
	}
	
	
	/**
	 * Layout the main GUI
	 */
	private void layoutPanels()
	{
		layoutTop();	
		layoutBottom();		
	}


	/**
	 * Layout the top two thirds of the main GUI
	 */
	private void layoutTop()
	{
		JPanel top = new JPanel(new GridLayout(2, 1, 10, 10)); 		
		
		JPanel innerTop = new JPanel(new BorderLayout()); 
		top.add(innerTop);
		JLabel dataLabel = new JLabel("Choose login data file     ");
		dataTextField = new JTextField(35);
		dataTextField.setPreferredSize(new Dimension(20, 20));
		chooseDataButton = new JButton("...");
		chooseDataButton.addActionListener(this);	
		innerTop.add(dataLabel, BorderLayout.WEST);
		innerTop.add(dataTextField);
		JPanel buttonPanel1 = new JPanel();
		buttonPanel1.add(chooseDataButton);
		innerTop.add(buttonPanel1, BorderLayout.EAST);			
		
		JPanel innerBottom = new JPanel(new BorderLayout()); 
		top.add(innerBottom);
		JLabel scheduleLabel = new JLabel("Choose schedule file      ");
		scheduleTextField = new JTextField(35);
		scheduleTextField.setPreferredSize(new Dimension(20, 20));
		chooseScheduleButton = new JButton("...");
		chooseScheduleButton.addActionListener(this);
		JPanel buttonPanel2 = new JPanel();
		buttonPanel2.add(chooseScheduleButton);
		innerBottom.add(scheduleLabel, BorderLayout.WEST);
		innerBottom.add(scheduleTextField);
		innerBottom.add(buttonPanel2, BorderLayout.EAST); 		
		
		add(top, BorderLayout.NORTH);
	}


	/**
	 * Layout the bottom section of the main GUI
	 */
	private void layoutBottom()
	{
		JPanel bottom = new JPanel();
		runButton = new JButton("Run");
		runButton.addActionListener(this);
		bottom.add(runButton);
		add(bottom, BorderLayout.SOUTH);
	}
	
	/**
	 * Launches the file chooser window that allows the user
	 * to select the required data and schedule files.
	 * Overloaded method that takes two file extension arguments. 
	 */
	private String launchFileChooser(String type, String extension1, String extension2)
	{
		JFileChooser chooser = new JFileChooser();
	    FileNameExtensionFilter filter = new FileNameExtensionFilter(type, extension1, extension2);
	    chooser.setFileFilter(filter);	     
	    chooser.showOpenDialog(CBSGUI.this);
	    
	    String path = "";
	    
	    try
	    {
	    	path = chooser.getSelectedFile().getCanonicalPath();
	    }
	    catch(IOException e)
	    {
	    	e.printStackTrace();
	    }
	    
	    return path;
	}
	
	
	/**
	 * Launches the file chooser window that allows the user
	 * to select the required data and schedule files.
	 * Overloaded method that takes one file extension argument. 
	 */
	private String launchFileChooser(String type, String extension)
	{
		JFileChooser chooser = new JFileChooser();
	    FileNameExtensionFilter filter = new FileNameExtensionFilter(type, extension);
	    chooser.setFileFilter(filter);	     
	    chooser.showOpenDialog(CBSGUI.this);
	    
	    String path = "";
	    
	    try
	    {
	    	path = chooser.getSelectedFile().getCanonicalPath();
	    }
	    catch(IOException e)
	    {
	    	e.printStackTrace();
	    }
	    
	    return path;
	}
	
	
	/**
	 * handles all click events
	 */
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == chooseDataButton)
		{			
			try
			{
				//display only CSV files and replace the back slashes from Windows file system
				chosenDataFile = launchFileChooser("CSV", "csv").replaceAll("\\\\", "/");
			}
			catch(NullPointerException n)
			{
				chosenDataFile = "";
			}
			
			if(!chosenDataFile.equals(""))
			{
				dataTextField.setText(chosenDataFile);
			}
			
		}
		if(e.getSource() == chooseScheduleButton)
		{
			try
			{
				//display only CSV files and replace the back slashes from Windows file system				
				chosenScheduleFile = launchFileChooser("CSV", "csv").replaceAll("\\\\", "/");
				//chosenScheduleFile = launchFileChooser("Excel", "xlsx", "xls").replaceAll("\\\\", "/");
			}
			catch(NullPointerException n)
			{
				chosenScheduleFile = "";
			}
			
			if(chosenScheduleFile != "")
			{
				scheduleTextField.setText(chosenScheduleFile);
			}
			
		}
		if(e.getSource() == runButton)
		{
			parser = new Controller(chosenDataFile, chosenScheduleFile);
			System.out.println("Program executed");
			System.exit(0);
		}
	}

	
	
}
