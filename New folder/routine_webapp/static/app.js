// Minimal DIU Routine Web Application Controller

const state = {
  theme: localStorage.getItem('diu_theme') || 'dark',
  currentTab: 'student',
  viewMode: 'day',
  selectedDay: 'Wednesday',
  dept: 'CSE',
  batch: '68',
  section: 'D',
  routine: [],
  departments: [],
  batches: [],
  sections: []
};

const WEEKDAYS = ['Saturday', 'Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday'];

// DOM Elements
const htmlEl = document.documentElement;
const themeToggleBtn = document.getElementById('themeToggleBtn');
const deptSelect = document.getElementById('deptSelect');
const batchSelect = document.getElementById('batchSelect');
const sectionSelect = document.getElementById('sectionSelect');
const dispSectionTag = document.getElementById('dispSectionTag');
const dispCoursesCount = document.getElementById('dispCoursesCount');
const dispClassesCount = document.getElementById('dispClassesCount');
const headerDeptBadge = document.getElementById('headerDeptBadge');
const instructorsBar = document.getElementById('instructorsBar');
const instructorsList = document.getElementById('instructorsList');
const daysNav = document.getElementById('daysNav');
const timelineContainer = document.getElementById('timelineContainer');
const weekMatrixContainer = document.getElementById('weekMatrixContainer');
const weekTableBody = document.getElementById('weekTableBody');
const auxViewContainer = document.getElementById('auxViewContainer');
const filterStrip = document.getElementById('filterStrip');
const quickSearchInput = document.getElementById('quickSearchInput');
const btnDayView = document.getElementById('btnDayView');
const btnWeekView = document.getElementById('btnWeekView');
const btnPrint = document.getElementById('btnPrint');

// Lifecycle
document.addEventListener('DOMContentLoaded', async () => {
  initTheme();
  initEvents();
  await loadDepartments();
  await loadBatchesAndSections();
  await fetchRoutine();
});

// Theme Toggle
function initTheme() {
  htmlEl.setAttribute('data-theme', state.theme);
  themeToggleBtn.addEventListener('click', () => {
    state.theme = state.theme === 'dark' ? 'light' : 'dark';
    htmlEl.setAttribute('data-theme', state.theme);
    localStorage.setItem('diu_theme', state.theme);
  });
}

// Event Listeners
function initEvents() {
  deptSelect.addEventListener('change', async (e) => {
    state.dept = e.target.value;
    headerDeptBadge.textContent = state.dept;
    await loadBatchesAndSections();
    await fetchRoutine();
  });

  batchSelect.addEventListener('change', async (e) => {
    state.batch = e.target.value;
    await updateSections();
    await fetchRoutine();
  });

  sectionSelect.addEventListener('change', async (e) => {
    state.section = e.target.value;
    await fetchRoutine();
  });

  btnDayView.addEventListener('click', () => switchView('day'));
  btnWeekView.addEventListener('click', () => switchView('week'));

  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      switchTab(btn.dataset.tab);
    });
  });

  window.addEventListener('keydown', (e) => {
    if (e.key === '/' && document.activeElement !== quickSearchInput) {
      e.preventDefault();
      quickSearchInput.focus();
    }
  });

  quickSearchInput.addEventListener('keypress', async (e) => {
    if (e.key === 'Enter') {
      const q = quickSearchInput.value.trim();
      if (q) handleSearch(q);
    }
  });

  btnPrint.addEventListener('click', () => window.print());

  const btnSyncLive = document.getElementById('btnSyncLive');
  const syncStatusText = document.getElementById('syncStatusText');
  const statusDot = document.querySelector('.status-dot');

  btnSyncLive.addEventListener('click', async () => {
    syncStatusText.textContent = "Syncing...";
    statusDot.classList.add('syncing');
    try {
      await fetch('/api/sync', { method: 'POST' });
      // Poll until finished
      setTimeout(async () => {
        await loadDepartments();
        await loadBatchesAndSections();
        await fetchRoutine();
        syncStatusText.textContent = "Live Synced";
        statusDot.classList.remove('syncing');
      }, 3000);
    } catch (err) {
      syncStatusText.textContent = "Sync Failed";
      statusDot.classList.remove('syncing');
    }
  });
}

function switchView(mode) {
  state.viewMode = mode;
  btnDayView.classList.toggle('active', mode === 'day');
  btnWeekView.classList.toggle('active', mode === 'week');

  daysNav.style.display = mode === 'day' ? 'flex' : 'none';
  timelineContainer.style.display = mode === 'day' ? 'flex' : 'none';
  weekMatrixContainer.style.display = mode === 'week' ? 'block' : 'none';

  if (mode === 'day') renderDaySchedule();
  else renderWeekSchedule();
}

async function switchTab(tab) {
  state.currentTab = tab;
  const isRoutine = tab === 'student';

  filterStrip.style.display = isRoutine ? 'flex' : 'none';
  instructorsBar.style.display = isRoutine ? 'flex' : 'none';
  daysNav.style.display = isRoutine && state.viewMode === 'day' ? 'flex' : 'none';
  timelineContainer.style.display = isRoutine && state.viewMode === 'day' ? 'flex' : 'none';
  weekMatrixContainer.style.display = isRoutine && state.viewMode === 'week' ? 'block' : 'none';
  auxViewContainer.style.display = isRoutine ? 'none' : 'block';

  if (isRoutine) {
    await fetchRoutine();
  } else if (tab === 'teacher') {
    renderFacultyView();
  } else if (tab === 'room') {
    renderRoomsView();
  } else if (tab === 'freeroom') {
    renderFreeRoomsView();
  }
}

// Data Loaders
async function loadDepartments() {
  const res = await fetch('/api/departments');
  const depts = await res.json();
  state.departments = depts;
  deptSelect.innerHTML = depts.map(d => 
    `<option value="${d.code}" ${d.code === state.dept ? 'selected' : ''}>${d.code} - ${d.name}</option>`
  ).join('');
}

async function loadBatchesAndSections() {
  const res = await fetch(`/api/batches?dept=${state.dept}`);
  const batches = await res.json();
  batches.sort((a, b) => b.name.localeCompare(a.name, undefined, { numeric: true }));
  state.batches = batches;

  batchSelect.innerHTML = batches.map(b => 
    `<option value="${b.name}" ${b.name === state.batch ? 'selected' : ''}>Batch ${b.name}</option>`
  ).join('');

  if (!batches.some(b => b.name === state.batch) && batches.length > 0) {
    state.batch = batches[0].name;
    batchSelect.value = state.batch;
  }

  await updateSections();
}

async function updateSections() {
  const res = await fetch(`/api/sections?dept=${state.dept}&batch=${state.batch}`);
  const secs = await res.json();
  secs.sort((a, b) => a.name.localeCompare(b.name));
  state.sections = secs;

  sectionSelect.innerHTML = secs.map(s => 
    `<option value="${s.name}" ${s.name === state.section ? 'selected' : ''}>Section ${s.name}</option>`
  ).join('');

  if (!secs.some(s => s.name === state.section) && secs.length > 0) {
    state.section = secs[0].name;
    sectionSelect.value = state.section;
  }
}

async function fetchRoutine() {
  const tag = `${state.dept}_${state.batch}_${state.section}`;
  dispSectionTag.textContent = tag;

  const res = await fetch(`/api/routine?dept=${state.dept}&batch=${state.batch}&section=${state.section}`);
  const data = await res.json();
  state.routine = data || [];

  const uniqueCourses = new Set(data.map(d => d.courseCode)).size;
  dispCoursesCount.textContent = uniqueCourses;
  dispClassesCount.textContent = data.length;

  // Instructors list
  const teachersMap = {};
  data.forEach(d => {
    if (d.teacherInitials) teachersMap[d.teacherInitials] = d.teacherName || d.teacherInitials;
  });

  instructorsList.innerHTML = Object.entries(teachersMap).map(([init, name]) => `
    <span class="tag-instructor" onclick="filterByTeacher('${init}')" title="${name}">${init}</span>
  `).join('');

  renderDaysNav();
  if (state.viewMode === 'day') renderDaySchedule();
  else renderWeekSchedule();
}

function renderDaysNav() {
  const counts = {};
  WEEKDAYS.forEach(w => counts[w] = 0);
  state.routine.forEach(r => { if (counts[r.weekday] !== undefined) counts[r.weekday]++; });

  daysNav.innerHTML = WEEKDAYS.map(day => {
    const isActive = day.toLowerCase() === state.selectedDay.toLowerCase();
    const count = counts[day] || 0;
    return `
      <button class="day-tab ${isActive ? 'active' : ''}" data-day="${day}">
        <span>${day}</span>
        <span class="day-badge">${count}</span>
      </button>
    `;
  }).join('');

  daysNav.querySelectorAll('.day-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      state.selectedDay = tab.dataset.day;
      renderDaysNav();
      renderDaySchedule();
    });
  });
}

function renderDaySchedule() {
  const classes = state.routine.filter(
    c => c.weekday.toLowerCase() === state.selectedDay.toLowerCase()
  );
  classes.sort((a, b) => a.startTime.localeCompare(b.startTime));

  if (classes.length === 0) {
    timelineContainer.innerHTML = `
      <div class="empty-placeholder">No classes scheduled on ${state.selectedDay}.</div>
    `;
    return;
  }

  timelineContainer.innerHTML = classes.map(c => {
    const isLab = c.courseTitle.toLowerCase().includes('lab') || c.room.includes('LAB');
    return `
      <div class="timeline-item">
        <div class="item-time">
          <div>${c.startTime}</div>
          <div style="color: var(--text-muted); font-size: 0.78rem;">${c.endTime}</div>
        </div>

        <div class="item-content">
          <div class="item-header">
            <span class="course-code">${c.courseCode}</span>
            <span class="type-pill ${isLab ? 'type-lab' : 'type-theory'}">${isLab ? 'Lab' : 'Theory'}</span>
          </div>
          <div class="course-title">${c.courseTitle}</div>
          <div class="instructor-info">
            Instructor: <strong>${c.teacherName || c.teacherInitials}</strong> (${c.teacherInitials})
          </div>
        </div>

        <div class="item-location">
          <div class="room-tag">${c.room}</div>
          <div class="building-tag">${c.building || 'Campus Tower'}</div>
        </div>
      </div>
    `;
  }).join('');
}

function renderWeekSchedule() {
  const timeSlots = ["8:30 AM", "10:00 AM", "11:30 AM", "1:00 PM", "2:30 PM", "4:00 PM"];

  weekTableBody.innerHTML = WEEKDAYS.map(day => {
    const dayEntries = state.routine.filter(e => e.weekday.toLowerCase() === day.toLowerCase());

    const slotCells = timeSlots.map(time => {
      const match = dayEntries.find(e => e.startTime.startsWith(time.split(' ')[0]));
      if (!match) return `<td></td>`;
      return `
        <td>
          <div class="table-slot">
            <span class="code">${match.courseCode}</span>
            <span class="room">${match.room}</span>
            <span class="faculty">${match.teacherInitials}</span>
          </div>
        </td>
      `;
    }).join('');

    return `<tr><td class="table-day">${day}</td>${slotCells}</tr>`;
  }).join('');
}

// Quick Search
async function handleSearch(q) {
  const res = await fetch(`/api/routine?query=${encodeURIComponent(q)}`);
  const data = await res.json();
  if (data && data.length > 0) {
    state.routine = data;
    dispSectionTag.textContent = q.toUpperCase();
    renderDaysNav();
    if (state.viewMode === 'day') renderDaySchedule();
    else renderWeekSchedule();
  } else {
    // Check teacher
    try {
      const resT = await fetch(`/api/teacher/${encodeURIComponent(q)}`);
      if (resT.ok) {
        const tData = await resT.json();
        state.routine = tData.classes || [];
        dispSectionTag.textContent = tData.name || q;
        renderDaysNav();
        renderDaySchedule();
        return;
      }
    } catch(e) {}
    alert(`No routine matches found for "${q}".`);
  }
}

// Faculty Directory
async function renderFacultyView() {
  const res = await fetch('/api/teachers');
  const list = await res.json();

  auxViewContainer.innerHTML = `
    <h3 style="margin-bottom: 16px; font-size: 1rem; font-weight: 600;">Faculty Directory</h3>
    <div class="grid-cards">
      ${list.map(t => `
        <div class="dir-card" onclick="filterByTeacher('${t.initials}')">
          <div class="title">${t.initials}</div>
          <div class="subtitle">${t.name}</div>
          <div style="font-size: 0.72rem; color: var(--accent); margin-top: 4px;">${t.total_classes} classes/wk</div>
        </div>
      `).join('')}
    </div>
  `;
}

window.filterByTeacher = async function(initials) {
  const res = await fetch(`/api/teacher/${initials}`);
  const data = await res.json();
  state.routine = data.classes || [];
  
  // Switch back to routine tab view smoothly
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === 'student'));
  state.currentTab = 'student';
  filterStrip.style.display = 'flex';
  instructorsBar.style.display = 'none';
  daysNav.style.display = 'flex';
  timelineContainer.style.display = 'flex';
  weekMatrixContainer.style.display = 'none';
  auxViewContainer.style.display = 'none';

  dispSectionTag.textContent = `${data.name || initials} (${initials})`;
  renderDaysNav();
  renderDaySchedule();
};

// Rooms View
async function renderRoomsView() {
  const res = await fetch('/api/rooms');
  const rooms = await res.json();

  auxViewContainer.innerHTML = `
    <h3 style="margin-bottom: 16px; font-size: 1rem; font-weight: 600;">Classroom & Lab Schedules</h3>
    <div class="grid-cards">
      ${rooms.map(r => `
        <div class="dir-card" onclick="filterByRoom('${r.room}')">
          <div class="title">${r.room}</div>
          <div class="subtitle">${r.total_classes} classes scheduled</div>
        </div>
      `).join('')}
    </div>
  `;
}

window.filterByRoom = async function(room) {
  const res = await fetch(`/api/room/${room}`);
  const data = await res.json();
  state.routine = data.schedule || [];

  document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === 'student'));
  state.currentTab = 'student';
  filterStrip.style.display = 'flex';
  instructorsBar.style.display = 'none';
  daysNav.style.display = 'flex';
  timelineContainer.style.display = 'flex';
  weekMatrixContainer.style.display = 'none';
  auxViewContainer.style.display = 'none';

  dispSectionTag.textContent = `Room ${room}`;
  renderDaysNav();
  renderDaySchedule();
};

// Free Rooms Finder
async function renderFreeRoomsView() {
  auxViewContainer.innerHTML = `
    <div style="display: flex; gap: 12px; align-items: flex-end; margin-bottom: 20px; flex-wrap: wrap;">
      <div class="filter-group">
        <label>Weekday</label>
        <select id="freeDaySelect">
          ${WEEKDAYS.map(w => `<option value="${w}">${w}</option>`).join('')}
        </select>
      </div>
      <div class="filter-group">
        <label>Time Slot</label>
        <select id="freeTimeSelect">
          <option value="">All Day Slots</option>
          <option value="8:30 AM">08:30 - 10:00 AM</option>
          <option value="10:00 AM">10:00 - 11:30 AM</option>
          <option value="11:30 AM">11:30 - 01:00 PM</option>
          <option value="1:00 PM">01:00 - 02:30 PM</option>
          <option value="2:30 PM">02:30 - 04:00 PM</option>
          <option value="4:00 PM">04:00 - 05:30 PM</option>
        </select>
      </div>
      <button class="tab-btn active" id="btnRunFreeSearch" style="padding: 6px 14px;">Find Empty Classrooms</button>
    </div>
    <div id="freeResultsGrid" class="grid-cards"></div>
  `;

  document.getElementById('btnRunFreeSearch').addEventListener('click', updateFreeRooms);
  await updateFreeRooms();
}

async function updateFreeRooms() {
  const day = document.getElementById('freeDaySelect').value;
  const time = document.getElementById('freeTimeSelect').value;
  const res = await fetch(`/api/free-rooms?weekday=${day}&time_slot=${encodeURIComponent(time)}`);
  const data = await res.json();

  const grid = document.getElementById('freeResultsGrid');
  if (data.free_rooms.length === 0) {
    grid.innerHTML = `<div class="empty-placeholder" style="grid-column: 1/-1;">No available rooms found for this slot.</div>`;
    return;
  }

  grid.innerHTML = data.free_rooms.map(r => `
    <div class="dir-card" onclick="filterByRoom('${r}')" style="text-align: center;">
      <div class="title" style="color: var(--tag-lab-text);">${r}</div>
      <div class="subtitle">Empty / Available</div>
    </div>
  `).join('');
}
