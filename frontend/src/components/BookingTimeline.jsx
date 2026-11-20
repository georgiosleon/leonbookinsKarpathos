import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
} from 'react';
import { bookingsForTimeline } from '../bookingConfig.js';
import { toTimelineItem } from '../bookingLogic.js';

const timelineOptions = {
  maxHeight: 2000,
  horizontalScroll: true,
  verticalScroll: true,
  zoomKey: 'ctrlKey',
  start: Date.now() - 2 * 24 * 60 * 60 * 1000,
  end: Date.now() + 17 * 24 * 60 * 60 * 1000,
  showCurrentTime: true,
  clickToUse: false,
  tooltip: { followMouse: true },
  stack: false,
  editable: { add: false, remove: true },
  orientation: { axis: 'both', item: 'top' },
  showMinorLabels: true,
  timeAxis: { scale: 'day', step: 1 },
  format: {
    minorLabels: {
      millisecond: '',
      second: '',
      minute: '',
      hour: '',
      weekday: '',
      day: 'DD ddd',
      month: '',
      year: '',
    },
  },
};

export const BookingTimeline = forwardRef(function BookingTimeline({
  mode,
  bookings,
  timelineType,
  title,
  description,
  onSelect,
  onDelete,
  onError,
}, ref) {
  const containerRef = useRef(null);
  const timelineRef = useRef(null);
  const itemsRef = useRef(null);
  const callbacksRef = useRef({ onSelect, onDelete, onError });
  const currentDateRef = useRef(new Date());

  callbacksRef.current = { onSelect, onDelete, onError };

  useImperativeHandle(ref, () => ({
    moveToDate(date) {
      currentDateRef.current = date;
      timelineRef.current?.moveTo(date, { animation: true });
    },
    moveToToday() {
      const date = new Date();
      currentDateRef.current = date;
      timelineRef.current?.moveTo(date, { animation: true });
    },
    shiftDays(days) {
      const date = new Date(currentDateRef.current.getTime() + days * 24 * 60 * 60 * 1000);
      currentDateRef.current = date;
      timelineRef.current?.moveTo(date, { animation: true });
    },
    clearSelection() {
      timelineRef.current?.setSelection([]);
    },
  }), []);

  useEffect(() => {
    const vis = window.vis;
    if (!vis?.Timeline || !vis?.DataSet) {
      callbacksRef.current.onError('The timeline library did not load.');
      return undefined;
    }

    const groups = new vis.DataSet(mode.resources.map(({ id, text }) => ({
      id,
      content: text,
    })));
    const items = new vis.DataSet();
    itemsRef.current = items;

    const timeline = new vis.Timeline(containerRef.current, items, groups, timelineOptions);
    timelineRef.current = timeline;
    timeline.setCurrentTime(Date.now());

    const handleClick = (properties) => {
      if (!properties.item) return;
      const item = items.get(properties.item);
      if (item?.recordData) callbacksRef.current.onSelect(item.recordData);
    };

    const handleRemove = (item, callback) => {
      const timelineItem = items.get(item.id);
      const booking = timelineItem?.recordData;
      if (!booking || !window.confirm(`Delete booking for ${booking.name}?`)) {
        callback(null);
        return;
      }

      callbacksRef.current.onDelete(booking.id)
        .then(() => callback(item))
        .catch((error) => {
          callbacksRef.current.onError(error.message);
          callback(null);
        });
    };

    timeline.on('click', handleClick);
    timeline.setOptions({ onRemove: handleRemove });

    return () => {
      timeline.off('click', handleClick);
      timeline.destroy();
      timelineRef.current = null;
      itemsRef.current = null;
    };
  }, [mode]);

  useEffect(() => {
    if (!itemsRef.current) return;
    const visibleBookings = bookingsForTimeline(bookings, timelineType);
    itemsRef.current.clear();
    if (visibleBookings.length) {
      itemsRef.current.add(visibleBookings.map((booking) => toTimelineItem(booking, mode)));
    }
  }, [bookings, mode, timelineType]);

  const visibleCount = bookingsForTimeline(bookings, timelineType).length;

  return (
    <section className={`panel timeline-panel timeline-panel-${timelineType}`} aria-labelledby={`timeline-heading-${timelineType}`}>
      <div className="panel-heading">
        <div>
          <h2 id={`timeline-heading-${timelineType}`}>{title} <span className="timeline-count">{visibleCount}</span></h2>
          <p>{description}</p>
        </div>
      </div>
      {visibleCount === 0 && <p className="timeline-empty">No {timelineType === 'cancelled' ? 'cancelled' : 'active or paid'} bookings.</p>}
      <div className="timeline-container" ref={containerRef} aria-label={`${mode.resourceLabel} ${title.toLowerCase()} timeline`} />
    </section>
  );
});
