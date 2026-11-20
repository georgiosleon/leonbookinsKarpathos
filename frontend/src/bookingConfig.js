const roomResources = [
  { id: 'APT2', text: 'APT 2' },
  { id: 'APT5', text: 'APT 5' },
  { id: 'APT6', text: 'APT 6' },
  { id: 'STU7', text: 'STU 7' },
  { id: 'STU8', text: 'STU 8' },
  { id: 'STU9', text: 'STU 9' },
  { id: 'STU10', text: 'STU 10' },
  { id: 'STU11', text: 'STU 11' },
  { id: 'STU12', text: 'STU 12' },
  { id: 'POT', text: 'POTIDEION' },
  { id: 'MER', text: 'MERTONAS' },
];

const vehicleResources = [
  ...Array.from({ length: 8 }, (_, index) => ({
    id: `CAR${index + 1}`,
    text: `CAR ${index + 1} 🚗`,
  })),
  ...Array.from({ length: 6 }, (_, index) => ({
    id: `BIKE${index + 1}`,
    text: `BIKE ${index + 1} 🛵`,
  })),
];

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
    resources: roomResources,
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
    resources: vehicleResources,
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
