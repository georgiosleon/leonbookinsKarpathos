const roomAgencies = [
  { id: 'abnb', text: 'AirBnB' },
  { id: 'bookingcom', text: 'Booking.com' },
  { id: 'walkin', text: 'Walk-in' },
  { id: 'closed', text: 'Closed' },
];

const vehicleAgencies = [
  { id: 'other', text: 'DEFAULT' },
  { id: 'abnb', text: 'RED' },
  { id: 'bookingcom', text: 'BLUE' },
  { id: 'walkin', text: 'GREEN' },
  { id: 'closed', text: 'Unavailable' },
];

export const bookingModes = {
  rooms: {
    id: 'rooms',
    title: 'Beds and Guests Planner',
    resourceLabel: 'Room',
    resources: [],
    agencies: roomAgencies,
    statuses: [
      { id: 'active', text: 'Active' },
      { id: 'cancelled', text: 'Cancelled' },
      { id: 'paid', text: 'Paid' },
    ],
    timelinePrefix: '🛌',
    timelineGuestLabel: '👨‍👩‍👦',
    unavailableMessage: 'Room is not available for those dates.',
  },
  cars: {
    id: 'cars',
    title: 'Cars and Guests Planner',
    resourceLabel: 'Vehicle',
    resources: [],
    agencies: vehicleAgencies,
    statuses: [
      { id: 'active', text: 'Active' },
      { id: 'cancelled', text: 'Cancelled' },
      { id: 'paid', text: 'Paid' },
    ],
    timelinePrefix: '',
    timelineGuestLabel: '👦',
    unavailableMessage: 'Vehicle is not available for those dates.',
  },
};

export function resourcesForMode(resources, mode) {
  return resources
    .filter((resource) => resource.type === mode.id)
    .map((resource) => ({
      id: resource.id,
      text: resource.name,
      active: resource.active,
      rentalTaxPerNight: resource.rentalTaxPerNight ?? 0,
    }));
}

export const guestCounts = [1, 2, 3, 4, 5, 6];
export const timelineStatuses = {
  bookable: ['active', 'paid'],
  cancelled: ['cancelled'],
};
export const isBookableStatus = (status) => timelineStatuses.bookable.includes(status);

export function bookingsForTimeline(bookings, timelineType) {
  const statuses = timelineStatuses[timelineType];
  if (!statuses) {
    throw new Error(`Unknown timeline type: ${timelineType}`);
  }
  return bookings.filter((booking) => statuses.includes(booking.status));
}

export function shouldStackTimeline(timelineType) {
  return timelineType === 'cancelled';
}
