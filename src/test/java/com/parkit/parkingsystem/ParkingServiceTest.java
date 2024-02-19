package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @AfterEach
    public void tearDownPerTest() {
        reset(inputReaderUtil, ticketDAO, parkingSpotDAO);
    }

    @Test
    public void processExitingVehicleTest() throws Exception {
        ArgumentCaptor<ParkingSpot> parkingSpotArgumentCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        ArgumentCaptor<Ticket> ticketArgumentCaptor = ArgumentCaptor.forClass(Ticket.class);

        parkingService.processExitingVehicle();

        verify(ticketDAO, Mockito.times(1)).getTicket(anyString());
        verify(ticketDAO, Mockito.times(1)).updateTicket(ticketArgumentCaptor.capture());
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(parkingSpotArgumentCaptor.capture());
        assertEquals(0, ticketDAO.getNbTicket(anyString()));
    }

    @Test
    public void testProcessIncomingVehicle() {
        ArgumentCaptor<ParkingSpot> parkingSpotArgumentCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        ArgumentCaptor<Ticket> ticketArgumentCaptor = ArgumentCaptor.forClass(Ticket.class);
        ArgumentCaptor<ParkingType> parkingTypeArgumentCaptor = ArgumentCaptor.forClass(ParkingType.class);

        when(parkingSpotDAO.getNextAvailableSlot(parkingTypeArgumentCaptor.capture())).thenReturn(1);
        when(ticketDAO.getNbTicket(anyString())).thenReturn(0);
        when(inputReaderUtil.readSelection()).thenReturn(1);

        parkingService.processIncomingVehicle();

        verify(ticketDAO, Mockito.times(1)).saveTicket(ticketArgumentCaptor.capture());
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(parkingSpotArgumentCaptor.capture());

        ParkingType capturedParkingType = parkingTypeArgumentCaptor.getValue();
        assertEquals(ParkingType.CAR, capturedParkingType);
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() {
        ArgumentCaptor<ParkingSpot> parkingSpotArgumentCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        ArgumentCaptor<Ticket> ticketArgumentCaptor = ArgumentCaptor.forClass(Ticket.class);
        when(ticketDAO.updateTicket(ticketArgumentCaptor.capture())).thenReturn(false);

        parkingService.processExitingVehicle();

        verify(ticketDAO, Mockito.times(1)).getTicket(any());
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any());
        verify(ticketDAO, Mockito.times(1)).updateTicket(ticketArgumentCaptor.capture());
        verify(parkingSpotDAO, never()).updateParking(parkingSpotArgumentCaptor.capture());

        boolean capturedUpdateTicket = ticketDAO.updateTicket(ticketArgumentCaptor.getValue());
        assertFalse(capturedUpdateTicket);
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() {
        ArgumentCaptor<ParkingType> parkingTypeArgumentCaptor = ArgumentCaptor.forClass(ParkingType.class);

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(parkingTypeArgumentCaptor.capture())).thenReturn(1);

        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(parkingTypeArgumentCaptor.capture());

        ParkingType capturedParkingType = parkingTypeArgumentCaptor.getValue();
        assertEquals(ParkingType.CAR, capturedParkingType);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertTrue(result.isAvailable());
        assertEquals(ParkingType.CAR, result.getParkingType());
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound () {
        ArgumentCaptor<ParkingType> parkingTypeArgumentCaptor = ArgumentCaptor.forClass(ParkingType.class);

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(parkingTypeArgumentCaptor.capture())).thenReturn(0);

        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        assertNull(result);
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        when(inputReaderUtil.readSelection()).thenReturn(3);

        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        assertNull(result);
    }
}
