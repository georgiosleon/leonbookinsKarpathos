// Local-only parser for pasted Booking.com confirmation emails.
// It returns form-shaped values for review; nothing is saved or sent anywhere.

const GREEK_SCRIPT = /[\u0370-\u03FF\u1F00-\u1FFF]/;

const MONTHS_EN = {
  january: 1, jan: 1,
  february: 2, feb: 2,
  march: 3, mar: 3,
  april: 4, apr: 4,
  may: 5,
  june: 6, jun: 6,
  july: 7, jul: 7,
  august: 8, aug: 8,
  september: 9, sep: 9, sept: 9,
  october: 10, oct: 10,
  november: 11, nov: 11,
  december: 12, dec: 12,
};

const MONTHS_EL = {
  'ιανουαρίου': 1, 'ιανάριος': 1, 'ιαν': 1,
  'φεβρουαρίου': 2, 'φεβρουάριος': 2, 'φεβ': 2,
  'μαρτίου': 3, 'μάρτιος': 3, 'μαρ': 3,
  'απριλίου': 4, 'απρίλιος': 4, 'απρ': 4,
  'μαΐου': 5, 'μάιος': 5, 'μαι': 5,
  'ιουνίου': 6, 'ιούνιος': 6, 'ιουν': 6,
  'ιουλίου': 7, 'ιούλιος': 7, 'ιουλ': 7,
  'αυγούστου': 8, 'αύγουστος': 8, 'αυγ': 8,
  'σεπτεμβρίου': 9, 'σέπτεμβριος': 9, 'σεπτ': 9, 'σεπ': 9,
  'οκτωβρίου': 10, 'οκτώβριος': 10, 'οκτ': 10,
  'νοεμβρίου': 11, 'νοέμβριος': 11, 'νοε': 11,
  'δεκεμβρίου': 12, 'δέκεμβριος': 12, 'δεκ': 12,
};

const LABELS = {
  en: {
    reservation: ['Booking number', 'Booking reference', 'Reservation number'],
    guest: ['Guest name', 'Guest(s)', 'Guests'],
    checkIn: ['Check-in'],
    checkOut: ['Check-out'],
    property: ['Property', 'Accommodation'],
    unit: ['Booked unit'],
    phone: ['Phone'],
    address: ['Address'],
    pin: ['PIN code'],
    total: ['Total price', 'Total amount'],
    maxGuests: ['Max guests'],
    guestEmail: ['Email', 'Email address'],
    guestPhone: ['Guest phone', 'Mobile phone'],
  },
  el: {
    reservation: ['Αριθμός κράτησης', 'Αριθμός αναφοράς κράτησης'],
    guest: ['Όνομα επισκέπτη', 'Επισκέπτης(-ες)'],
    checkIn: ['Check-in', 'Άφιξη'],
    checkOut: ['Check-out', 'Αναχώρηση'],
    property: ['Κατάλυμα'],
    unit: ['Μονάδα που κρατήθηκε'],
    phone: ['Τηλέφωνο'],
    address: ['Διεύθυνση'],
    pin: ['Κωδικός PIN'],
    total: ['Συνολική τιμή', 'Συνολικό ποσό'],
    maxGuests: ['Μέγιστος αριθμός επισκεπτών'],
    guestEmail: ['Διεύθυνση email', 'Ηλ. ταχυδρομείο'],
    guestPhone: ['Τηλέφωνο επισκέπτη'],
  },
};

const DATE_PATTERN = /(\d{1,2})\s+([\p{L}]+\.?)\s+(\d{4})/gu;
const OCCUPANCY_EN = /(?:^|[^\p{L}\d])(\d{1,2})\s+adults?(?![\p{L}])/iu;
const OCCUPANCY_EL = /(?:^|[^\p{L}\d])(\d{1,2})\s+ενήλικες?(?![\p{L}])/iu;
const OCCUPANCY_PATTERN = /(?:^|[^\p{L}\d])(\d{1,2})\s+(?:adults?|ενήλικες?)(?![\p{L}])/iu;
const MONEY_PATTERN = /(?:€|EUR)\s*[\d.,]+/iu;

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function detectLanguage(text) {
  return GREEK_SCRIPT.test(text) ? 'el' : 'en';
}

function resolveLanguage(text, override) {
  if (override === 'en' || override === 'el') return override;
  return detectLanguage(text);
}

function labelsFor(language) {
  if (language === 'en' || language === 'el') return LABELS[language];
  return Object.fromEntries(
    Object.keys(LABELS.en).map((key) => [key, [...LABELS.en[key], ...LABELS.el[key]]]),
  );
}

function monthsFor(language) {
  if (language === 'en') return MONTHS_EN;
  if (language === 'el') return MONTHS_EL;
  return { ...MONTHS_EN, ...MONTHS_EL };
}

function occupancyPatternFor(language) {
  if (language === 'en') return OCCUPANCY_EN;
  if (language === 'el') return OCCUPANCY_EL;
  return OCCUPANCY_PATTERN;
}

function labelledValue(lines, labels) {
  for (let index = 0; index < lines.length; index += 1) {
    for (const label of labels) {
      const match = lines[index].match(new RegExp(`^\\s*${escapeRegExp(label)}\\s*:\\s*(.*?)\\s*$`, 'iu'));
      if (!match) continue;
      if (match[1]) return match[1].trim();
      // Some templates put the value on the following line (e.g. "Booked unit:").
      const next = lines.slice(index + 1).find((line) => line.trim() && !/^\s*[^:]+:\s*/u.test(line));
      if (next) return next.trim();
    }
  }
  return '';
}

function hasLabel(lines, labels) {
  return labelledValue(lines, labels) !== '';
}

function labelledEntry(lines, labels) {
  for (const line of lines) {
    for (const label of labels) {
      const match = line.match(new RegExp(`^\\s*${escapeRegExp(label)}\\s*:\\s*(.*?)\\s*$`, 'iu'));
      if (match && match[1]) return { label, value: match[1].trim() };
    }
  }
  return { label: '', value: '' };
}

function toIsoDate(day, monthWord, year, months) {
  const month = months[String(monthWord).replace(/\.$/, '').toLowerCase()];
  const numericDay = Number(day);
  if (!month || !/^\d{4}$/.test(year) || numericDay < 1 || numericDay > 31) return '';
  const date = new Date(Date.UTC(Number(year), month - 1, numericDay));
  if (date.getUTCFullYear() !== Number(year) || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== numericDay) return '';
  return `${year}-${String(month).padStart(2, '0')}-${String(numericDay).padStart(2, '0')}`;
}

function extractDates(text, months) {
  const dates = [];
  for (const match of text.matchAll(DATE_PATTERN)) {
    const iso = toIsoDate(match[1], match[2], match[3], months);
    if (iso) dates.push(iso);
  }
  return dates;
}

function parseDateValue(value, months) {
  const withoutTimes = String(value).replace(/\([^)]*\)/g, ' ');
  const withoutWeekday = withoutWeekdayPrefix(withoutTimes);
  return extractDates(withoutWeekday, months)[0] || '';
}

function withoutWeekdayPrefix(value) {
  return value.replace(/^\s*[^,;0-9]+[,;]\s*/u, '').trim();
}

function parseGuestCount(lines, labels, pattern) {
  for (const line of lines) {
    if (hasLabel([line], labels.maxGuests)) continue;
    if (hasLabel([line], labels.pin) || hasLabel([line], labels.total)) continue;
    const match = line.match(pattern);
    if (match) return Number(match[1]);
  }
  return 0;
}

function subjectLine(lines) {
  return lines.find((line) => /^\s*subject\s*:/iu.test(line))
    ?? lines.find((line) => /booking\.com/iu.test(line))
    ?? '';
}

function splitGuests(value) {
  return value.split(',').map((part) => part.trim()).filter(Boolean);
}

/**
 * Parse a pasted Booking.com confirmation into reviewable form values.
 * @param {string} text raw email text, optionally including a Subject: line
 * @param {'auto'|'en'|'el'} override forced language mode
 */
export function parseBookingEmail(text, override = 'auto') {
  const raw = String(text ?? '').replace(/\r\n?/g, '\n').replace(/\u00a0/g, ' ');
  const language = resolveLanguage(raw, override);
  const labels = labelsFor(language);
  const months = monthsFor(language);
  const lines = raw.split('\n');
  const warnings = [];
  const notes = [];
  const values = {};

  const isBookingCom = /booking\.com/iu.test(raw);
  const reservationId = labelledValue(lines, labels.reservation);
  const guestValue = labelledValue(lines, labels.guest);
  const guestNames = guestValue ? splitGuests(guestValue) : [];
  const checkIn = parseDateValue(labelledValue(lines, labels.checkIn), months);
  const checkOut = parseDateValue(labelledValue(lines, labels.checkOut), months);
  const guestCount = parseGuestCount(lines, labels, occupancyPatternFor(language));
  const guestEmail = labelledValue(lines, labels.guestEmail);
  const guestPhone = labelledValue(lines, labels.guestPhone);
  const propertyName = labelledValue(lines, labels.property);
  const unitName = labelledValue(lines, labels.unit);
  const propertyPhone = labelledValue(lines, labels.phone);
  const propertyAddress = labelledValue(lines, labels.address);
  const pin = labelledValue(lines, labels.pin);
  const total = labelledEntry(lines, labels.total);

  let startDate = checkIn;
  let endDate = checkOut;
  if (!startDate || !endDate) {
    const subjectDates = extractDates(subjectLine(lines), months);
    if (!startDate) startDate = subjectDates[0] || '';
    if (!endDate) endDate = subjectDates[1] || '';
  }

  if (reservationId) values.platformReservationId = reservationId;
  if (guestNames.length) values.name = guestNames.join(', ');
  if (startDate) values.startDate = startDate;
  if (endDate) values.endDate = endDate;
  if (guestCount) values.numOfGuests = String(guestCount);
  if (guestEmail) values.email = guestEmail;
  if (guestPhone) values.tel = guestPhone;

  const hasDetails = reservationId || guestNames.length || startDate || endDate;
  if (isBookingCom && hasDetails) values.agency = 'bookingcom';

  if (total.value) {
    const amount = total.value.match(MONEY_PATTERN)?.[0]?.trim() || total.value;
    notes.push({ label: total.label, value: amount });
  }

  if (!isBookingCom || !hasDetails) warnings.push('NOTHING_FOUND');
  if (!reservationId) warnings.push('RESERVATION_ID_MISSING');
  if (!guestNames.length) warnings.push('GUEST_NAME_MISSING');
  if (!startDate || !endDate) warnings.push('DATES_MISSING');
  if (!guestCount) warnings.push('GUEST_COUNT_MISSING');
  if (propertyPhone) warnings.push('PROPERTY_PHONE_IGNORED');
  if (propertyAddress) warnings.push('PROPERTY_ADDRESS_IGNORED');
  if (pin) warnings.push('PIN_IGNORED');
  if (notes.length) warnings.push('MONEY_NOT_FILLED');
  if (guestNames.length > 1) warnings.push('MULTIPLE_GUESTS');

  return {
    detectedLanguage: language,
    values,
    notes,
    warnings,
    propertyName,
    unitName,
    multipleGuests: guestNames.length > 1,
  };
}
