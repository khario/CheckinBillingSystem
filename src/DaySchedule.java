/**
 * This class models a given day's schedule by way of an 
 * open bucket hash table. Each departure's airline code 
 * is hashed and its data is stored in a Flight object as 
 * part of a linked list.
 * 
 * @author Khari
 */

import java.util.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DaySchedule {

	private final int NUMBUCKETS = 57; //number of buckets in each OBHT

	//amount of extra time in minutes that the airlines are allowed to be logged 
	//before and after the allotted time for a flight
	private final int GRACEPERIOD = 10; 
	private final int HOURLYCHARGE = 100; 
	private Flight[] buckets;	//hash table
	

	public DaySchedule()
	{
		buckets = new Flight[NUMBUCKETS];
	}
	
	
	
	/**
	 * Adds new flights to their relevant bucket based on their airline code.
	 * New flights are added to the tail of the linked list in order to 
	 * maintain chronological order of the scheduled flights.
	 */
	public void add(Flight newFlight)
	{		
		int bucketIndex = hash(newFlight.getAirlineCode());
		
		Flight curr = buckets[bucketIndex];

		if(curr != null)
		{
			try
			{
				while(curr.getNext() != null)
				{
					curr = curr.getNext();					
				}
				
				curr.setNext(newFlight);
				curr.getNext().setPrevious(curr);	

				setScheduledLinks(newFlight);
			}
			catch(NullPointerException e)
			{
				e.printStackTrace();
			}
		}
		else 
		{
			buckets[bucketIndex] = newFlight;
		}		
	}
	

	/**
	 * This accounts for the possibility of collisions 
	 * in the hash table by allowing each flight to 
	 * keep references to its next and previous
	 * flights with matching airline codes; in 
	 * addition to the standard 'next' and 'previous'
	 * used to keep track of the linked list 
	 * @param newFlight
	 */
	private void setScheduledLinks(Flight newFlight)
	{
		Flight curr = newFlight.getPrevious();

		while(curr != null)
		{
			if(curr.getAirlineCode().equals(newFlight.getAirlineCode()))
			{
				curr.setScheduledNext(newFlight);
				newFlight.setScheduledPrevious(curr);		
				break;
			}
			else
			{
				curr = curr.getPrevious(); 
			}
		}
	}
	
	
	/**
	 * Checks the hash table for the presence of  
	 * nodes with the given airline code
	 */
	private boolean hasAirlineCode(String code)
	{
		int bucketIndex = hash(code);		
		Flight curr = buckets[bucketIndex];
		
		while(curr != null)
		{
			if(curr.getAirlineCode().equals(code)) 
			{
				return true;
			}
			curr = curr.getNext();
		}		
		return false;
	}
	
	
	
	/**
	 * Checks an individual line item from the data file,
	 * compares the logged in period to the day's schedule
	 * of flights for the relevant airline.
	 * The method determines the number of minutes in which
	 * the airline was logged in outside of their allotted 
	 * time and calculates a charge accordingly.
	 * @param line
	 * @return String array with line item data for report
	 */
	public String[] processRow(String[] line) 
	{
		String workstation = line[0];
		String code = line[1];
		String counter = convertCounterName(workstation);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");
		DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("M/d/yy H:mm");
		LocalDateTime dateTime;	
		
		try
		{
			dateTime = LocalDateTime.parse(line[2], formatter);
		}
		catch(java.time.format.DateTimeParseException e)
		{
			dateTime = LocalDateTime.parse(line[2], formatter2);
		}
		
		LocalDate date = dateTime.toLocalDate();			
		LocalTime loginTime = dateTime.toLocalTime();
		LocalTime logoutTime = loginTime.plusMinutes(Integer.parseInt(line[3]));
		
		String[] result = new String[7];
		
		//the time span in minutes during which an airline can be 
		//logged in to check passengers in without being charged a fee
		int timeAllowed = 180;  
		
		//reduce the time allowed to 45 minutes for the gates
		/*
		switch(workstation)
		{
		case "GND1GTG001": case "GND1GTG002": case "GND1GTG003": case "GND1GTG004": 
			timeAllowed = 45;
		default: break;
		}
		*/
		
		// tally of the number of chargeable minutes
		int rowTotal = 0;

		//check whether the hash table has at least 
		//one entry for this row's airline code
		if(hasAirlineCode(code))
		{
			int bucketIndex = hash(code);			
			Flight curr = buckets[bucketIndex];			
			int charge = 0;			
			
			if(isOverlappingAny(curr, loginTime, logoutTime, timeAllowed, code))
			{
				while(curr != null)
				{
					if(curr.getAirlineCode().equals(code) && isActiveFlight(curr, date)) 
					{
						if((isOverlapping(loginTime, logoutTime, curr.getDepTime(), timeAllowed)) && (!isValidSession(loginTime, logoutTime, curr.getDepTime(), timeAllowed)))
						{
							//if surpassing a valid period on both ends
							if(loggedInEarly(loginTime, curr.getDepTime(), timeAllowed) && loggedOutLate(logoutTime, curr.getDepTime(), timeAllowed))
							{																
								LocalTime effectiveLoginTime = loginTime;
								LocalTime effectiveLogoutTime = logoutTime;
								
								effectiveLoginTime = adjustEffectiveLoginTime(curr, effectiveLoginTime, rowTotal, timeAllowed);
								effectiveLogoutTime = adjustEffectiveLogoutTime(curr, effectiveLogoutTime, timeAllowed);								

	
								//add the number of minutes in violation before the start 
								//of the valid period for the current flight
								rowTotal += (int)effectiveLoginTime.until(curr.getDepTime().minusMinutes(timeAllowed+GRACEPERIOD), ChronoUnit.MINUTES);
								
								/*
								if the next flight is so soon that the beginning of its valid
								period is before the effective login time, add 0 to the rowTotal
								as opposed to the resulting negative number
								*/
								rowTotal += Math.max((int)curr.getDepTime().plusMinutes(GRACEPERIOD).until(effectiveLogoutTime, ChronoUnit.MINUTES), 0);								
							}

							else if(loggedInEarly(loginTime, curr.getDepTime(), timeAllowed) && !(loggedOutLate(logoutTime, curr.getDepTime(), timeAllowed)))
							{								
								LocalTime effectiveLoginTime = loginTime;
								
								effectiveLoginTime = adjustEffectiveLoginTime(curr, effectiveLoginTime, rowTotal, timeAllowed);								
								
								//add the number of minutes in violation before the start 
								//of the valid period for the current flight
								rowTotal += (int)effectiveLoginTime.until(curr.getDepTime().minusMinutes(timeAllowed+GRACEPERIOD), ChronoUnit.MINUTES);								
							}

							else if(loggedOutLate(logoutTime, curr.getDepTime(), timeAllowed) && !(loggedInEarly(loginTime, curr.getDepTime(), timeAllowed)))
							{
								LocalTime effectiveLogoutTime = logoutTime;
								
								effectiveLogoutTime = adjustEffectiveLogoutTime(curr, effectiveLogoutTime, timeAllowed);
								
								/*
								if the next flight is so soon that the beginning of its valid
								period is before the effective login time, add 0 to the rowTotal
								as opposed to the resulting negative number
								*/
								rowTotal += Math.max((int)curr.getDepTime().plusMinutes(GRACEPERIOD).until(effectiveLogoutTime, ChronoUnit.MINUTES), 0);								
							}
						}
					}
					curr = curr.getScheduledNext();
				}

				int chargeableHours = 0;
				if(rowTotal != 0)
				{
					chargeableHours = (rowTotal/60)+1;
				}
					
				charge = chargeableHours*HOURLYCHARGE;
				result[0] = counter;
				result[1] = code;
				result[2] = loginTime.toString();
				result[3] = logoutTime.toString();
				result[4] = Integer.toString(rowTotal);
				result[5] = Integer.toString(chargeableHours);
				result[6] = Integer.toString(charge);
			
				return result;
					
			}

			else //logged-in time does not intersect with any valid period
			{
				//include entire duration without condition
				rowTotal = (int)(loginTime.until(logoutTime, ChronoUnit.MINUTES));
				int chargeableHours = (rowTotal/60) +1;
				
				charge = chargeableHours*HOURLYCHARGE;
				result[0] = counter;
				result[1] = code;
				result[2] = loginTime.toString();
				result[3] = logoutTime.toString();
				result[4] = Integer.toString(rowTotal);
				result[5] = Integer.toString(chargeableHours);
				result[6] = Integer.toString(charge);
				
				return result;
			}				
		}

		else
		{
			return null;
		}
	}
	
	
	/**
	 * takes the workstation name as input and 
	 * returns the corresponding counter name.
	 * @param workstation
	 * @return counter name
	 */
	private String convertCounterName(String workstation) 
	{
		switch(workstation)
		{
		case "GND1CKB001": case "GND1CKR002":
			return "Counter 1";
		case "GND1CKB003": case "GND1CKR004":
			return "Counter 2";
		case "GND1CKB005": case "GND1CKR006":
			return "Counter 3";
		case "GND1CKB007": case "GND1CKR008":
			return "Counter 4";
		case "GND1CKB009": case "GND1CKR010":
			return "Counter 5";
		case "GND1CKB011": case "GND1CKR012":
			return "Counter 6";
		case "GND1CKB013": case "GND1CKR014":
			return "Counter 7";
		case "GND1CKB015": case "GND1CKR016":
			return "Counter 8";
		case "GND1CKB017": case "GND1CKR018":
			return "Counter 9";
		case "GND1CKB019": case "GND1CKR020":
			return "Counter 10";
		case "GND1CKB021": case "GND1CKR022":
			return "Counter 11";
		case "GND1CKB023": case "GND1CKR024":
			return "Counter 12";
		case "GND1CKB025": case "GND1CKR026":
			return "Counter 13";
		case "GND1CKB027": case "GND1CKR028":
			return "Counter 14";
		case "GND1CKB029": case "GND1CKR030":
			return "Counter 15";
		case "GND1GTG001":
			return "Gate 1";
		case "GND1GTG002":
			return "Gate 2";
		case "GND1GTG003":
			return "Gate 3";
		case "GND1GTG004":
			return "Gate 4";
		default:
			return "Invalid workstation";
		}
		
	}
	
	
	/**
	 * if a portion of the charge has already been accounted for,
	 * or there is an overlap between the valid time periods for 
	 * this flight and the previous one, adjust the effective login 
	 * time to the end of that period
	 * @param curr
	 * @param effectiveLoginTime
	 * @param rowTotal
	 * @param timeAllowed
	 */
	private LocalTime adjustEffectiveLoginTime(Flight curr, LocalTime effectiveLoginTime, int rowTotal, int timeAllowed)
	{
		if(curr.getScheduledPrevious() != null)				
		{
			if(rowTotal != 0 || (curr.getScheduledPrevious().getDepTime().plusMinutes(GRACEPERIOD).until(curr.getDepTime(), ChronoUnit.MINUTES) <= timeAllowed))
			{
				effectiveLoginTime = curr.getDepTime().minusMinutes(timeAllowed+GRACEPERIOD);
				return effectiveLoginTime;
			}									
		}
		return effectiveLoginTime;
	}
	
	
	/**
	 * if the logout time is later than the start of the valid 
	 * period for the next flight in the list, set the effective 
	 * logout time to the start of that period to avoid 
	 * double-charging
	 * @param curr
	 * @param effectiveLogoutTime
	 * @param timeAllowed
	 */
	private LocalTime adjustEffectiveLogoutTime(Flight curr, LocalTime effectiveLogoutTime, int timeAllowed)
	{
		if(curr.getScheduledNext() != null)
		{
			if(effectiveLogoutTime.compareTo(curr.getScheduledNext().getDepTime().minusMinutes(timeAllowed + GRACEPERIOD)) > 0)
			{
				effectiveLogoutTime = curr.getScheduledNext().getDepTime().minusMinutes(timeAllowed + GRACEPERIOD);
				return effectiveLogoutTime;
			}
		}
		return effectiveLogoutTime;
	}
	
	
	/**
	 * Checks whether the login session being evaluated 
	 * overlaps any of the nodes in the list.
	 * @param node
	 * @param login
	 * @param logout
	 * @param timeAllowed
	 * @param code
	 * @return
	 */
	private boolean isOverlappingAny(Flight flight, LocalTime login, LocalTime logout, int timeAllowed, String code)
	{
		Flight curr = flight;		
		while(curr != null)
		{
			if(curr.getAirlineCode().equals(code) && isOverlapping(login, logout, curr.getDepTime(), timeAllowed)) 
			{
				return true;
			}
			curr = curr.getNext();
		}
				
		return false;
	}
	
	
	
	/**
	 * Checks whether a given time period has any overlap with 
	 * any scheduled period for that airline on that day of week
	 * @param login
	 * @param logout
	 * @param scheduled
	 * @param timeAllowed
	 * @return
	 */
	private boolean isOverlapping(LocalTime login, LocalTime logout, LocalTime scheduled, int timeAllowed)
	{
		return (login.isBefore(scheduled.plusMinutes(GRACEPERIOD)) && scheduled.minusMinutes(timeAllowed+GRACEPERIOD).isBefore(logout));
	}


	/**
	 * Checks whether both the login and logout times 
	 * are within a valid period
	 * @param login
	 * @param logout
	 * @param scheduled
	 * @param timeAllowed
	 * @return
	 */
	private boolean isValidSession(LocalTime login, LocalTime logout, LocalTime scheduled, int timeAllowed)
	{
		return (login.isAfter(scheduled.minusMinutes(timeAllowed+GRACEPERIOD)) && logout.isBefore(scheduled.plusMinutes(GRACEPERIOD)));
	}


	/**
	 * Checks whether the login time is 
	 * before the valid period.
	 * @param login
	 * @param scheduled
	 * @param timeAllowed
	 * @return
	 */
	private boolean loggedInEarly(LocalTime login, LocalTime scheduled, int timeAllowed)
	{
		return (login.isBefore(scheduled.minusMinutes(timeAllowed+GRACEPERIOD)));
	}
	
	
	/**
	 * Checks whether the logout time is 
	 * after the valid period.
	 * @param logout
	 * @param scheduled
	 * @param timeAllowed
	 * @return
	 */
	private boolean loggedOutLate(LocalTime logout, LocalTime scheduled, int timeAllowed)
	{
		return (logout.isAfter(scheduled.plusMinutes(GRACEPERIOD)));
	}
	
	
	/**
	 * Checks whether the flight being compared to the
	 * login record was active on the day of login.
	 * @param curr
	 * @param date
	 * @return
	 */
	private boolean isActiveFlight(Flight curr, LocalDate date)
	{
		return (date.isAfter(curr.getStartDate().minusDays(1)) && date.isBefore(curr.getEndDate().plusDays(1)));
	}
	
	
	/**
	 * hash function used to assign airline codes
	 * to buckets in the DaySchedule hash table
	 * @param airlineCode
	 * @return
	 */
	private int hash(String airlineCode)
	{
		return Math.abs(airlineCode.hashCode()) % NUMBUCKETS;
	}
	
	
}
