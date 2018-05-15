/**
 * 
 */

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Flight implements Comparable<Flight>{ 
	private String flightNum;
	private String airlineCode;
	private int[] daysOfOperation;
	private String daysOfOperationString;
	private LocalTime depTime;
	private LocalDate startDate, endDate;
	private String[] rawData;
	
	private Flight next;
	private Flight previous;
	private Flight scheduledPrevious;
	private Flight scheduledNext;

	private final int FLIGHTNUMCOL = 42;
	private final int DAYSOFWEEKCOL = 45;
	private final int TIMECOL = 46;
	private final int STARTDATECOL = 47;
	private final int ENDDATECOL = 48;
	
	
	public Flight(String[] record)
	{
		rawData = record;
		flightNum = record[FLIGHTNUMCOL];
		initDaysOfOperation(record[DAYSOFWEEKCOL]);
		initAirlineCode();
		initDepTime(record[TIMECOL]);
		initDates(record[STARTDATECOL], record[ENDDATECOL]);
		
		next = null;
		previous = null;
		scheduledNext = null;
		scheduledPrevious = null;		
		
	}
	
	
	
	private void initDaysOfOperation(String days)
	{
		daysOfOperationString = days;
		
		// create a schedule entry for each day specified 
		// with the current airline code and time.
		daysOfOperation = new int[days.length()];
		for(int i = 0; i < daysOfOperation.length; i++)
		{
			daysOfOperation[i] = Integer.parseInt(String.valueOf(days.charAt(i)));
		}
	}



	private void initAirlineCode()
	{
		String[] flightNumber = flightNum.split(" ");
		airlineCode = flightNumber[0];
	}
	

	private void initDepTime(String time)
	{
		String dTimeString = LocalTime.parse(time,  
				DateTimeFormatter.ofPattern("H:mm:ss")).format(DateTimeFormatter.ofPattern("HH:mm"));
		depTime = LocalTime.parse(dTimeString);
	}


	private void initDates(String start, String end)
	{
		startDate = LocalDate.parse(start, DateTimeFormatter.ofPattern("M/d/yyyy"));
		endDate = LocalDate.parse(end, DateTimeFormatter.ofPattern("M/d/yyyy"));
	}

	public String toString()
	{
		return flightNum + " | " + airlineCode + " | " + daysOfOperationString + " | " + depTime + " | " + startDate + " | " + endDate;
	}


	public int compareTo(Flight otherFlight)
	{
		return this.getDepTime().compareTo(otherFlight.getDepTime());
	}
	
	
	

	public String[] getRawData() {
		return rawData;
	}



	public String getFlightNum() {
		return flightNum;
	}



	public String getAirlineCode() {
		return airlineCode;
	}



	public int[] getDaysOfOperation() {
		return daysOfOperation;
	}



	public LocalTime getDepTime() {
		return depTime;
	}



	public LocalDate getStartDate() {
		return startDate;
	}



	public LocalDate getEndDate() {
		return endDate;
	}



	public Flight getNext() {
		return next;
	}



	public void setNext(Flight nextFlight) {
		next = nextFlight;
	}



	public Flight getPrevious() {
		return previous;
	}



	public void setPrevious(Flight previousFlight) {
		previous = previousFlight;
	}



	public Flight getScheduledPrevious() {
		return scheduledPrevious;
	}



	public void setScheduledPrevious(Flight scheduledPreviousFlight) {
		scheduledPrevious = scheduledPreviousFlight;
	}



	public Flight getScheduledNext() {
		return scheduledNext;
	}



	public void setScheduledNext(Flight scheduledNextFlight) {
		scheduledNext = scheduledNextFlight;
	}	

}
