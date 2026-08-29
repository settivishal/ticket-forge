/**
 * TicketForge — Interactive Glassmorphism Dashboard Engine
 * Real-time SSE event stream, REST & GraphQL client, dynamic 2D seat map
 */

(function () {
    'use strict';

    // Application State
    const state = {
        authRole: 'dev-customer', // 'dev-customer', 'dev-vip', 'dev-admin'
        seats: [],
        systemStatus: null,
        waitlist: [],
        eventSource: null,
        selectedSeat: null
    };

    // DOM Elements
    const elements = {
        roleSelector: document.getElementById('roleSelector'),
        sseStatusBadge: document.getElementById('sseStatusBadge'),
        sseStatusText: document.getElementById('sseStatusText'),
        statTotalSeats: document.getElementById('statTotalSeats'),
        statAvailableSeats: document.getElementById('statAvailableSeats'),
        statAvailablePct: document.getElementById('statAvailablePct'),
        statReservedSeats: document.getElementById('statReservedSeats'),
        statHeldSeats: document.getElementById('statHeldSeats'),
        statWaitlistCount: document.getElementById('statWaitlistCount'),
        occupancyRate: document.getElementById('occupancyRate'),
        barReserved: document.getElementById('barReserved'),
        barHeld: document.getElementById('barHeld'),
        barAvailable: document.getElementById('barAvailable'),
        seatGrid: document.getElementById('seatGrid'),
        btnRefreshSeats: document.getElementById('btnRefreshSeats'),
        inputUserId: document.getElementById('inputUserId'),
        selectPriority: document.getElementById('selectPriority'),
        holdTtlInput: document.getElementById('holdTtlInput'),
        btnReserveSeat: document.getElementById('btnReserveSeat'),
        btnHoldSeat: document.getElementById('btnHoldSeat'),
        btnFlashBurst: document.getElementById('btnFlashBurst'),
        waitlistContainer: document.getElementById('waitlistContainer'),
        btnRefreshWaitlist: document.getElementById('btnRefreshWaitlist'),
        tabWaitlistBadge: document.getElementById('tabWaitlistBadge'),
        adminInitSeatsInput: document.getElementById('adminInitSeatsInput'),
        btnAdminInitialize: document.getElementById('btnAdminInitialize'),
        adminExpandSeatsInput: document.getElementById('adminExpandSeatsInput'),
        btnAdminExpand: document.getElementById('btnAdminExpand'),
        adminReleaseFrom: document.getElementById('adminReleaseFrom'),
        adminReleaseTo: document.getElementById('adminReleaseTo'),
        btnAdminReleaseRange: document.getElementById('btnAdminReleaseRange'),
        eventFeed: document.getElementById('eventFeed'),
        btnClearLog: document.getElementById('btnClearLog'),
        seatModal: document.getElementById('seatModal'),
        modalSeatTitle: document.getElementById('modalSeatTitle'),
        modalSeatNumber: document.getElementById('modalSeatNumber'),
        modalSeatTier: document.getElementById('modalSeatTier'),
        modalSeatStatus: document.getElementById('modalSeatStatus'),
        modalOccupantRow: document.getElementById('modalOccupantRow'),
        modalSeatOccupant: document.getElementById('modalSeatOccupant'),
        modalActionButtons: document.getElementById('modalActionButtons'),
        btnCloseModal: document.getElementById('btnCloseModal'),
        gqlOutput: document.getElementById('gqlOutput'),
        btnGqlSystemStatus: document.getElementById('btnGqlSystemStatus'),
        btnGqlSeats: document.getElementById('btnGqlSeats'),
        btnGqlWaitlist: document.getElementById('btnGqlWaitlist')
    };

    // Helper: Headers for API requests based on selected Auth Role
    function getAuthHeaders(extraHeaders = {}) {
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...extraHeaders
        };

        if (state.authRole === 'dev-admin') {
            headers['Authorization'] = 'Bearer dev-admin';
        } else if (state.authRole === 'dev-vip') {
            headers['Authorization'] = 'Bearer dev-customer';
            headers['X-Dev-Priority'] = '3';
        } else {
            headers['Authorization'] = 'Bearer dev-customer';
        }
        return headers;
    }

    // 1. Initialize Real-Time SSE Stream
    function initEventStream() {
        if (state.eventSource) {
            state.eventSource.close();
        }

        try {
            state.eventSource = new EventSource('/api/v1/events/stream');

            state.eventSource.onopen = function () {
                updateSseStatus(true, 'SSE Stream Live');
                logSystemEvent('Connected to real-time SSE stream at /api/v1/events/stream');
            };

            state.eventSource.addEventListener('INIT', function (e) {
                logSystemEvent('Stream established: ' + e.data);
            });

            state.eventSource.addEventListener('DOMAIN_EVENT', function (e) {
                try {
                    const event = JSON.parse(e.data);
                    handleIncomingDomainEvent(event);
                } catch (err) {
                    console.error('Error parsing domain event:', err);
                }
            });

            state.eventSource.onerror = function () {
                updateSseStatus(false, 'Reconnecting...');
            };
        } catch (err) {
            console.error('SSE initialization error:', err);
            updateSseStatus(false, 'SSE Offline');
        }
    }

    function updateSseStatus(isConnected, text) {
        if (isConnected) {
            elements.sseStatusBadge.className = 'status-pill connected';
        } else {
            elements.sseStatusBadge.className = 'status-pill disconnected';
        }
        elements.sseStatusText.textContent = text;
    }

    function handleIncomingDomainEvent(event) {
        appendEventFeed(event);
        // Automatically sync metrics & seat map on state-changing events
        fetchSystemStatus();
        fetchSeats();
        fetchWaitlist();
    }

    function appendEventFeed(event) {
        const entry = document.createElement('div');
        entry.className = `event-entry ${event.eventType || 'SYSTEM'}`;

        const time = new Date(event.timestamp || Date.now()).toLocaleTimeString();
        const badge = `<span class="event-badge ${event.eventType}">${event.eventType}</span>`;
        const seatInfo = event.seatNumber ? `[Seat #${event.seatNumber}]` : '';
        const userInfo = event.userId ? `(User: ${event.userId})` : '';

        entry.innerHTML = `
            <span class="event-time">${time}</span>
            ${badge}
            <span class="event-msg">${event.message || ''} ${seatInfo} ${userInfo}</span>
        `;

        elements.eventFeed.appendChild(entry);
        elements.eventFeed.scrollTop = elements.eventFeed.scrollHeight;
    }

    function logSystemEvent(msg) {
        const entry = document.createElement('div');
        entry.className = 'event-entry system-msg';
        const time = new Date().toLocaleTimeString();
        entry.innerHTML = `
            <span class="event-time">${time}</span>
            <span class="event-badge EXPANDED">SYSTEM</span>
            <span class="event-msg">${msg}</span>
        `;
        elements.eventFeed.appendChild(entry);
        elements.eventFeed.scrollTop = elements.eventFeed.scrollHeight;
    }

    // 2. Fetch System Status (KPI Cards & Progress Bar)
    async function fetchSystemStatus() {
        try {
            const res = await fetch('/api/v1/seats/availability');
            if (!res.ok) return;
            const json = await res.json();
            const data = json.data;
            state.systemStatus = data;

            elements.statTotalSeats.textContent = data.totalSeats;
            elements.statAvailableSeats.textContent = data.availableSeats;
            elements.statReservedSeats.textContent = data.reservedSeats;
            elements.statHeldSeats.textContent = data.heldSeats;
            elements.statWaitlistCount.textContent = data.waitlistCount;
            elements.tabWaitlistBadge.textContent = data.waitlistCount;

            const total = data.totalSeats || 1;
            const availPct = Math.round((data.availableSeats / total) * 100);
            elements.statAvailablePct.textContent = `${availPct}% inventory available`;

            const reservedPct = (data.reservedSeats / total) * 100;
            const heldPct = (data.heldSeats / total) * 100;
            const availableBarPct = (data.availableSeats / total) * 100;

            elements.barReserved.style.width = `${reservedPct}%`;
            elements.barHeld.style.width = `${heldPct}%`;
            elements.barAvailable.style.width = `${availableBarPct}%`;

            const bookedPct = Math.round(((data.reservedSeats + data.heldSeats) / total) * 100);
            elements.occupancyRate.textContent = `${bookedPct}% Booked`;
        } catch (err) {
            console.error('Failed to fetch system status:', err);
        }
    }

    // 3. Fetch Seats & Render 2D Grid
    async function fetchSeats() {
        try {
            const res = await fetch('/api/v1/seats');
            if (!res.ok) return;
            const json = await res.json();
            const seats = json.data || [];
            state.seats = seats;
            renderSeatGrid(seats);
        } catch (err) {
            console.error('Failed to fetch seats:', err);
        }
    }

    function renderSeatGrid(seats) {
        elements.seatGrid.innerHTML = '';

        if (!seats || seats.length === 0) {
            elements.seatGrid.innerHTML = '<p class="empty-state">No seats initialized. Go to Admin tab to initialize venue capacity.</p>';
            return;
        }

        seats.forEach(seat => {
            const node = document.createElement('div');
            node.className = `seat-node ${seat.status}`;
            node.id = `seat-${seat.seatNumber}`;
            node.dataset.seatNumber = seat.seatNumber;

            const tierCode = seat.tier ? seat.tier.substring(0, 3) : 'STD';
            node.innerHTML = `
                <span class="seat-num">#${seat.seatNumber}</span>
                <span class="seat-tier-badge">${tierCode}</span>
            `;

            node.addEventListener('click', () => openSeatModal(seat));
            elements.seatGrid.appendChild(node);
        });
    }

    // 4. Fetch Waitlist
    async function fetchWaitlist() {
        try {
            const res = await fetch('/api/v1/waitlist', { headers: getAuthHeaders() });
            if (!res.ok) return;
            const json = await res.json();
            const waitlist = json.data || [];
            state.waitlist = waitlist;
            renderWaitlist(waitlist);
        } catch (err) {
            console.error('Failed to fetch waitlist:', err);
        }
    }

    function renderWaitlist(waitlist) {
        if (!waitlist || waitlist.length === 0) {
            elements.waitlistContainer.innerHTML = '<p class="empty-state">No users currently in waitlist.</p>';
            return;
        }

        let html = `
            <table class="waitlist-table">
                <thead>
                    <tr>
                        <th>Pos</th>
                        <th>User ID</th>
                        <th>Priority</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
        `;

        waitlist.forEach(entry => {
            html += `
                <tr>
                    <td class="waitlist-pos">#${entry.queuePosition}</td>
                    <td><strong>${entry.userId}</strong></td>
                    <td><span class="badge-priority">Tier ${entry.priority}</span></td>
                    <td>
                        <button class="btn-secondary-xs" onclick="window.upgradeUserPriority('${entry.userId}', ${entry.priority + 1})">Promote</button>
                        <button class="btn-secondary-xs" onclick="window.leaveWaitlist('${entry.userId}')">Exit</button>
                    </td>
                </tr>
            `;
        });

        html += '</tbody></table>';
        elements.waitlistContainer.innerHTML = html;
    }

    // Global Action Helpers for Inline Table Actions
    window.upgradeUserPriority = async function (userId, newPriority) {
        const priority = Math.min(5, newPriority);
        try {
            const res = await fetch(`/api/v1/waitlist/${userId}`, {
                method: 'PATCH',
                headers: getAuthHeaders(),
                body: JSON.stringify({ newPriority: priority })
            });
            if (res.ok) {
                fetchWaitlist();
                logSystemEvent(`Promoted ${userId} to Priority Tier ${priority}`);
            }
        } catch (err) {
            console.error(err);
        }
    };

    window.leaveWaitlist = async function (userId) {
        try {
            const res = await fetch(`/api/v1/waitlist/${userId}`, {
                method: 'DELETE',
                headers: getAuthHeaders()
            });
            if (res.ok) {
                fetchWaitlist();
                fetchSystemStatus();
                logSystemEvent(`${userId} exited the waitlist.`);
            }
        } catch (err) {
            console.error(err);
        }
    };

    // 5. Seat Detail Quick Modal
    function openSeatModal(seat) {
        state.selectedSeat = seat;
        elements.modalSeatTitle.textContent = `Seat #${seat.seatNumber}`;
        elements.modalSeatNumber.textContent = `#${seat.seatNumber}`;
        elements.modalSeatTier.textContent = seat.tier || 'STANDARD';
        elements.modalSeatStatus.textContent = seat.status;
        elements.modalSeatStatus.className = `badge-status ${seat.status}`;

        if (seat.occupantUserId) {
            elements.modalOccupantRow.style.display = 'flex';
            elements.modalSeatOccupant.textContent = seat.occupantUserId;
        } else {
            elements.modalOccupantRow.style.display = 'none';
        }

        // Dynamic Action Buttons based on status
        elements.modalActionButtons.innerHTML = '';
        if (seat.status === 'AVAILABLE') {
            const btnBook = document.createElement('button');
            btnBook.className = 'btn-primary';
            btnBook.textContent = 'Reserve This Seat';
            btnBook.onclick = () => {
                bookSpecificSeat(seat.seatNumber);
                closeModal();
            };
            elements.modalActionButtons.appendChild(btnBook);
        } else {
            const btnCancel = document.createElement('button');
            btnCancel.className = 'btn-danger';
            btnCancel.textContent = 'Cancel Reservation';
            btnCancel.onclick = () => {
                cancelSeat(seat.seatNumber, seat.occupantUserId || elements.inputUserId.value);
                closeModal();
            };
            elements.modalActionButtons.appendChild(btnCancel);
        }

        elements.seatModal.classList.remove('hidden');
    }

    function closeModal() {
        elements.seatModal.classList.add('hidden');
        state.selectedSeat = null;
    }

    // 6. User Actions (Booking, Hold, Burst)
    async function reserveSeat() {
        const userId = elements.inputUserId.value.trim() || 'usr_fan_' + Math.floor(Math.random() * 1000);
        const priority = parseInt(elements.selectPriority.value, 10) || 1;

        try {
            const res = await fetch('/api/v1/reservations', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ userId, priority })
            });
            const json = await res.json();
            if (res.status === 201) {
                logSystemEvent(`✅ Success! Seat #${json.data.seatNumber} reserved for ${userId}`);
            } else if (res.status === 202) {
                logSystemEvent(`⏳ Venue full. ${userId} joined priority waitlist.`);
            } else {
                logSystemEvent(`⚠️ ${json.detail || json.message || 'Booking error'}`);
            }
            fetchSystemStatus();
            fetchSeats();
            fetchWaitlist();
        } catch (err) {
            console.error('Reservation error:', err);
        }
    }

    async function holdSeat() {
        const userId = elements.inputUserId.value.trim() || 'usr_fan_' + Math.floor(Math.random() * 1000);
        const priority = parseInt(elements.selectPriority.value, 10) || 1;
        const ttl = parseInt(elements.holdTtlInput.value, 10) || 60;

        // Use GraphQL hold mutation for hold operation
        const query = `
            mutation {
                holdSeat(userId: "${userId}", priority: ${priority}, ttlSeconds: ${ttl}) {
                    seatNumber
                    userId
                    tier
                    expiresAt
                }
            }
        `;
        try {
            const res = await fetch('/graphql', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ query })
            });
            const json = await res.json();
            if (json.data && json.data.holdSeat) {
                logSystemEvent(`⏱️ Held Seat #${json.data.holdSeat.seatNumber} for ${userId} (TTL ${ttl}s)`);
            } else {
                logSystemEvent(`⚠️ Hold failed or placed on waitlist.`);
            }
            fetchSystemStatus();
            fetchSeats();
            fetchWaitlist();
        } catch (err) {
            console.error('Hold error:', err);
        }
    }

    async function bookSpecificSeat(seatNumber) {
        await reserveSeat();
    }

    async function cancelSeat(seatNumber, userId) {
        try {
            const res = await fetch(`/api/v1/reservations/${seatNumber}?userId=${encodeURIComponent(userId || '')}`, {
                method: 'DELETE',
                headers: getAuthHeaders()
            });
            const json = await res.json();
            if (res.ok) {
                logSystemEvent(`🗑️ Reservation for Seat #${seatNumber} cancelled.`);
            } else {
                logSystemEvent(`⚠️ Cancellation error: ${json.detail || json.message}`);
            }
            fetchSystemStatus();
            fetchSeats();
            fetchWaitlist();
        } catch (err) {
            console.error('Cancellation error:', err);
        }
    }

    async function simulateFlashBurst() {
        logSystemEvent('🚀 Firing high-concurrency burst of 10 parallel reservation requests...');
        const promises = [];
        for (let i = 1; i <= 10; i++) {
            const uid = 'burst_fan_' + Math.floor(Math.random() * 9000 + 1000);
            const prio = (i % 3) + 1;
            promises.push(
                fetch('/api/v1/reservations', {
                    method: 'POST',
                    headers: getAuthHeaders(),
                    body: JSON.stringify({ userId: uid, priority: prio })
                })
            );
        }
        await Promise.allSettled(promises);
        fetchSystemStatus();
        fetchSeats();
        fetchWaitlist();
    }

    // 7. Admin Actions
    async function adminInitialize() {
        const count = parseInt(elements.adminInitSeatsInput.value, 10) || 50;
        try {
            const res = await fetch('/api/v1/seats/initialize', {
                method: 'POST',
                headers: getAuthHeaders({ 'Authorization': 'Bearer dev-admin' }),
                body: JSON.stringify({ seatCount: count })
            });
            if (res.ok) {
                logSystemEvent(`👑 Venue re-initialized with ${count} seats.`);
                fetchSystemStatus();
                fetchSeats();
                fetchWaitlist();
            }
        } catch (err) {
            console.error('Admin initialize error:', err);
        }
    }

    async function adminExpand() {
        const count = parseInt(elements.adminExpandSeatsInput.value, 10) || 10;
        try {
            const res = await fetch('/api/v1/seats/expand', {
                method: 'POST',
                headers: getAuthHeaders({ 'Authorization': 'Bearer dev-admin' }),
                body: JSON.stringify({ additionalSeats: count })
            });
            if (res.ok) {
                logSystemEvent(`👑 Expanded capacity by ${count} seats.`);
                fetchSystemStatus();
                fetchSeats();
                fetchWaitlist();
            }
        } catch (err) {
            console.error('Admin expand error:', err);
        }
    }

    async function adminReleaseRange() {
        const from = elements.adminReleaseFrom.value.trim();
        const to = elements.adminReleaseTo.value.trim();
        if (!from || !to) return;
        try {
            const res = await fetch('/api/v1/reservations/release-range', {
                method: 'POST',
                headers: getAuthHeaders({ 'Authorization': 'Bearer dev-admin' }),
                body: JSON.stringify({ fromUserId: from, toUserId: to })
            });
            const json = await res.json();
            if (res.ok) {
                logSystemEvent(`👑 Batch released ${json.data ? json.data.length : 0} seats for range [${from}, ${to}].`);
                fetchSystemStatus();
                fetchSeats();
                fetchWaitlist();
            }
        } catch (err) {
            console.error('Admin release range error:', err);
        }
    }

    // 8. GraphQL Query Buttons
    async function executeGraphQl(query) {
        try {
            elements.gqlOutput.textContent = 'Executing query...';
            const res = await fetch('/graphql', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query })
            });
            const json = await res.json();
            elements.gqlOutput.textContent = JSON.stringify(json, null, 2);
        } catch (err) {
            elements.gqlOutput.textContent = 'GraphQL Error: ' + err.message;
        }
    }

    // 9. Tab Switching Logic
    function initTabs() {
        const tabButtons = document.querySelectorAll('.tab-btn');
        tabButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                tabButtons.forEach(b => b.classList.remove('active'));
                document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

                btn.classList.add('active');
                const target = document.getElementById(btn.dataset.tab);
                if (target) target.classList.add('active');
            });
        });
    }

    // 10. Event Listeners Registration
    function initEventListeners() {
        elements.roleSelector.addEventListener('change', (e) => {
            state.authRole = e.target.value;
            logSystemEvent(`Switched active role to ${state.authRole}`);
            if (state.authRole === 'dev-admin') {
                elements.inputUserId.value = 'dev_admin';
            } else if (state.authRole === 'dev-vip') {
                elements.inputUserId.value = 'usr_vip_' + Math.floor(Math.random() * 100);
                elements.selectPriority.value = '3';
            } else {
                elements.inputUserId.value = 'usr_fan_' + Math.floor(Math.random() * 100);
                elements.selectPriority.value = '1';
            }
        });

        elements.btnRefreshSeats.addEventListener('click', () => {
            fetchSystemStatus();
            fetchSeats();
        });

        elements.btnReserveSeat.addEventListener('click', reserveSeat);
        elements.btnHoldSeat.addEventListener('click', holdSeat);
        elements.btnFlashBurst.addEventListener('click', simulateFlashBurst);
        elements.btnRefreshWaitlist.addEventListener('click', fetchWaitlist);

        elements.btnAdminInitialize.addEventListener('click', adminInitialize);
        elements.btnAdminExpand.addEventListener('click', adminExpand);
        elements.btnAdminReleaseRange.addEventListener('click', adminReleaseRange);

        elements.btnCloseModal.addEventListener('click', closeModal);
        elements.seatModal.addEventListener('click', (e) => {
            if (e.target === elements.seatModal) closeModal();
        });

        elements.btnClearLog.addEventListener('click', () => {
            elements.eventFeed.innerHTML = '';
        });

        // GraphQL buttons
        elements.btnGqlSystemStatus.addEventListener('click', () => {
            executeGraphQl('query { systemStatus { totalSeats availableSeats heldSeats reservedSeats waitlistCount } }');
        });
        elements.btnGqlSeats.addEventListener('click', () => {
            executeGraphQl('query { seats { seatNumber status tier occupantUserId } }');
        });
        elements.btnGqlWaitlist.addEventListener('click', () => {
            executeGraphQl('query { waitlist { queuePosition userId priority status } }');
        });
    }

    // Initialize Application
    document.addEventListener('DOMContentLoaded', () => {
        initTabs();
        initEventListeners();
        initEventStream();
        fetchSystemStatus();
        fetchSeats();
        fetchWaitlist();
    });

})();
