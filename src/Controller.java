/**
 * This class reads and stores the data from both
 * the file that details the counter logins for 
 * the month and the file that details the month's
 * flight schedule.
 */

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Controller {
	private Set<String> airlineSet; // a set for all airline codes that appear in the month's report
	private String dataFilepath; // filepath of the SITA report
	private String scheduleFilepath; // filepath of the flight schedule
	private String report; // the resulting billing report
	private String outputFilename = "Report.csv";
	private List<Flight> flights;
	private DaySchedule[] schedules; // array of daily schedules based on the schedule file
	private final int ARRDEPCOL = 43;

	
	
	public Controller(String data, String schedule)
	{
		airlineSet = new HashSet<String>();
		dataFilepath = data;
		scheduleFilepath = schedule;	
		schedules = new DaySchedule[7];
		flights = new ArrayList<Flight>();
		initAirlineSet();
		initDaySchedules();	
		processCharges();
	}
	
	
	/**
	 * Reads in the SITA report and adds the airline codes to the set
	 */
	private void initAirlineSet()
	{
		try
		{
			FileReader reader = new FileReader(dataFilepath);
			Scanner scanner = new Scanner(reader);
			
			while(scanner.hasNextLine())
			{
				String temp = scanner.nextLine();
				String[] tempArray = temp.split(",");
				
				//exclude any blank lines at the end of CSV file
				if(tempArray[0].charAt(0) == 'W' || tempArray[0].charAt(0) == 'G')
				{
					if(tempArray[1].length() < 4)
					{
						airlineSet.add(tempArray[1]);
					}
				}								
			} 			
			scanner.close();
			reader.close();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		} 
	}
	
	
	/**
	 * Reads in the schedule file and adds each scheduled departure to 
	 * its corresponding DaySchedule
	 */
	private void initDaySchedules()
	{
		// initialise the 7 schedule objects
		for(int i = 0; i < schedules.length; i++)
		{
			schedules[i] = new DaySchedule();
		}
		
		try
		{
			FileReader reader = new FileReader(scheduleFilepath);
			Scanner scanner = new Scanner(reader);
			
			while(scanner.hasNextLine()) 
			{
				String temp = scanner.nextLine();
				String[] record = temp.split(",");
				
				// skip first line and only check the departure rows
				if(record[0].charAt(0) == 'M' && record[ARRDEPCOL].equals("D"))
				{
					//add each flight to the ArrayList before sorting 
					//them according to departure time
					flights.add(new Flight(record));
				}
			}
			scanner.close();
			reader.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		//Sort the flight list by time
		Collections.sort(flights);
		
		for(Flight flight : flights)
		{
			//use the flight's getDaysOfWeek() method to create another for loop 
			//to add each flight to it's respective DaySchedule			
			for(int i = 0; i < flight.getDaysOfOperation().length; i++)
			{
				schedules[(flight.getDaysOfOperation()[i])-1].add(new Flight(flight.getRawData()));
			}			
		}		
	}
	
	
	
	/**
	 * For each airline code in airlineSet, this method takes each matching 
	 * line item in the SITA report, has it processed by its corresponding 
	 * DaySchedule and then produces a cumulative report file with all 
	 * resulting charges.
	 */
	private void processCharges()
	{
		report = "";
		StringBuilder builder = new StringBuilder(report);

		//Format headings for report
		builder.append(String.format("%-14s" + "," + "%-14s" + "," + "%-10s" + "," + "%-10s" + "," + "%-10s" + "," 
		+ "%-10s" + "," + "%-16s" + "," +  "%-16s" + "," + "%-10s", "DATE",  "COUNTER", "AIRLINE", "LOGIN", 
		"LOGOUT", "DURATION", "BILLED MINUTES", "BILLED HOURS", "CHARGE"));
		builder.append("\r\n");
		// builder.append("--------------------------------------------------------------------------------");
		// builder.append("\r\n");

		//process the data file one airline at a time
		for(String code : airlineSet)
		{
			int airlineTotal = 0; //tally of charges for current airline
			try
			{
				FileReader reader = new FileReader(dataFilepath);
				Scanner scanner = new Scanner(reader);
				scanner.nextLine(); //skip the header row of the data file
				
				while(scanner.hasNextLine())
				{
					String line = scanner.nextLine();
					String[] tempArray = line.split(",");
					
					//exclude any blank lines at the end of CSV file
					if(tempArray[0].charAt(0) == 'W' || tempArray[0].charAt(0) == 'G')
					{
						if(tempArray[1].equals(code)) 
						{
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");
							DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("M/d/yy H:mm");
							
							LocalDateTime dateTime;
							
							try
							{
								dateTime = LocalDateTime.parse(tempArray[2], formatter);
							}
							catch(java.time.format.DateTimeParseException e)
							{
								dateTime = LocalDateTime.parse(tempArray[2], formatter2);
							}
							
							
							int dayOfWeek = dateTime.getDayOfWeek().getValue()-1;
							

							//go to the flight schedule for the given day of week and process the current row.
							//This returns a String array with any applicable charges and other related info.							
							String[] chargedItems = schedules[dayOfWeek].processRow(tempArray);
							
							
							//chargedItems[6] = the amount charged.
							if(chargedItems != null && Integer.parseInt(chargedItems[6]) > 0 
									&& !(chargedItems[0].charAt(0) == 'G'))
							{
								airlineTotal += Integer.parseInt(chargedItems[6]);
								int duration = Integer.parseInt(tempArray[3]);
								
								builder.append(String.format("%-14s" + "," + "%-14s" + "," + "%-10s" + "," + "%-10s" 
								+ "," + "%-10s" + "," + "%-10s" + "," + "%-16s" + "," + "%-16s" + "," + /*"$" +*/ "%-10s",
										dateTime.toLocalDate().toString(), chargedItems[0], chargedItems[1], chargedItems[2], 
										chargedItems[3], duration, chargedItems[4], chargedItems[5], chargedItems[6]));
								
								builder.append("\r\n");
							}							
						}
					}							
				} 
				
				scanner.close();
				reader.close();
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			} 
			
			builder.append("," + "," + "," + "," + "," + "," + "," + "TOTAL CHARGE FOR " + code + ": " + "," + /*"$" +*/ airlineTotal + "\r\n\r\n\r\n");
		}
		report = builder.toString();
		
		try
		{
			FileWriter writer = new FileWriter(outputFilename);
			writer.write(report);
			writer.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}		
	}


	public DaySchedule[] getSchedules() {
		return schedules;
	}
	
	

}
	
	

