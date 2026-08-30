/**
 * TicketForge — Modern Enterprise Ticketing Engine UI
 * Ticketmaster-inspired interface with real-time SSE stream, Supabase OAuth2 / Dev Mock authentication,
 * interactive 2D seating chart, customer & admin roles, and dynamic digital passes.
 */

(function () {
    'use strict';

    // Client Authentication Configuration (Fetched from backend)
    let authConfig = {
        isDev: true,
        supabaseUrl: 'https://mksdjnpmljjjrevywutt.supabase.co',
        supabaseAnonKey: ''
    };

    // Application State
    const state = {
        currentUser: {
            id: 'usr_alex',
            name: 'Alex Miller',
            email: 'alex@ticketforge.local',
            role: 'CUSTOMER', // 'CUSTOMER' or 'ADMIN'
            priority: 3 // 1: Standard, 2: Premium, 3: VIP
        },
        token: null, // Supabase RS256 JWT access token
        activeView: 'CUSTOMER', // 'CUSTOMER' or 'ADMIN'
        seats: [],
        systemStatus: null,
        waitlist: [],
        selectedSeat: null,
        eventSource: null,
        holdInterval: null,
        holdSecondsRemaining: 0
    };

    // DOM Elements Cache
    const el = {
        // Navigation & Auth
        tabCustomerPortal: document.getElementById('tabCustomerPortal'),
        tabAdminConsole: document.getElementById('tabAdminConsole'),
        liveStatusBadge: document.getElementById('liveStatusBadge'),
        liveStatusText: document.getElementById('liveStatusText'),
        btnUserAccount: document.getElementById('btnUserAccount'),
        navUserAvatar: document.getElementById('navUserAvatar'),
        navUserName: document.getElementById('navUserName'),
        navUserRole: document.getElementById('navUserRole'),
        
        // Views
        customerView: document.getElementById('customerView'),
        adminView: document.getElementById('adminView'),

        // Telemetry & Metrics
        statAvailableCount: document.getElementById('statAvailableCount'),
        statReservedCount: document.getElementById('statReservedCount'),
        statHeldCount: document.getElementById('statHeldCount'),
        statWaitlistCount: document.getElementById('statWaitlistCount'),
        statOccupancyPct: document.getElementById('statOccupancyPct'),
        barReserved: document.getElementById('barReserved'),
        barHeld: document.getElementById('barHeld'),
        barAvailable: document.getElementById('barAvailable'),

        // Customer Seating & Booking
        seatGrid: document.getElementById('seatGrid'),
        btnSyncSeats: document.getElementById('btnSyncSeats'),
        selectedSeatBox: document.getElementById('selectedSeatBox'),
        summarySeatNum: document.getElementById('summarySeatNum'),
        summarySeatPrice: document.getElementById('summarySeatPrice'),
        summarySeatStatus: document.getElementById('summarySeatStatus'),
        summarySeatTier: document.getElementById('summarySeatTier'),
        summaryFanName: document.getElementById('summaryFanName'),
        selectedTierBadge: document.getElementById('selectedTierBadge'),
        btnReserveSeat: document.getElementById('btnReserveSeat'),
        btnHoldSeat: document.getElementById('btnHoldSeat'),
        holdTimerBox: document.getElementById('holdTimerBox'),
        holdCountdownVal: document.getElementById('holdCountdownVal'),
        userWaitlistTier: document.getElementById('userWaitlistTier'),
        btnJoinWaitlistDirect: document.getElementById('btnJoinWaitlistDirect'),
        myTicketsContainer: document.getElementById('myTicketsContainer'),

        // Admin Console
        adminInitCount: document.getElementById('adminInitCount'),
        btnAdminInitVenue: document.getElementById('btnAdminInitVenue'),
        adminExpandCount: document.getElementById('adminExpandCount'),
        btnAdminExpandVenue: document.getElementById('btnAdminExpandVenue'),
        adminReleaseFrom: document.getElementById('adminReleaseFrom'),
        adminReleaseTo: document.getElementById('adminReleaseTo'),
        btnAdminReleaseRange: document.getElementById('btnAdminReleaseRange'),
        btnAdminBurstTest: document.getElementById('btnAdminBurstTest'),
        burstStatusMsg: document.getElementById('burstStatusMsg'),
        adminWaitlistTableContainer: document.getElementById('adminWaitlistTableContainer'),
        btnAdminRefreshWaitlist: document.getElementById('btnAdminRefreshWaitlist'),

        // Auth Modal & Forms
        authModal: document.getElementById('authModal'),
        btnCloseAuthModal: document.getElementById('btnCloseAuthModal'),
        tabSignIn: document.getElementById('tabSignIn'),
        tabSignUp: document.getElementById('tabSignUp'),
        tabAdminAuth: document.getElementById('tabAdminAuth'),
        authAlertMsg: document.getElementById('authAlertMsg'),
        signInForm: document.getElementById('signInForm'),
        signUpForm: document.getElementById('signUpForm'),
        adminAuthForm: document.getElementById('adminAuthForm'),
        loginEmail: document.getElementById('loginEmail'),
        loginPassword: document.getElementById('loginPassword'),
        signupName: document.getElementById('signupName'),
        signupEmail: document.getElementById('signupEmail'),
        signupPassword: document.getElementById('signupPassword'),
        signupPriority: document.getElementById('signupPriority'),
        adminAuthEmail: document.getElementById('adminAuthEmail'),
        adminAuthPassword: document.getElementById('adminAuthPassword'),
        btnSubmitSignIn: document.getElementById('btnSubmitSignIn'),
        btnSubmitSignUp: document.getElementById('btnSubmitSignUp'),
        btnSubmitAdminAuth: document.getElementById('btnSubmitAdminAuth'),

        // Activity Drawer & Toasts
        btnOpenDrawer: document.getElementById('btnOpenDrawer'),
        btnCloseDrawer: document.getElementById('btnCloseDrawer'),
        drawerBackdrop: document.getElementById('drawerBackdrop'),
        drawerFeed: document.getElementById('drawerFeed'),
        btnClearActivityFeed: document.getElementById('btnClearActivityFeed'),
        toastContainer: document.getElementById('toastContainer')
    };

    // Helper: Dynamic Tier Details & Pricing
    function getTierInfo(seatNumber, tierName) {
        if (tierName === 'VIP' || (seatNumber && seatNumber <= 10)) {
            return { name: 'VIP Pass', price: '$250.00', className: 'vip', code: 'VIP' };
        } else if (tierName === 'PREMIUM' || (seatNumber && seatNumber <= 30)) {
            return { name: 'Premium Center', price: '$120.00', className: 'premium', code: 'PREM' };
        } else {
            return { name: 'Standard Arena', price: '$65.00', className: 'standard', code: 'STD' };
        }
    }

    // Helper: Build API Request Headers
    function getAuthHeaders(extraHeaders = {}) {
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...extraHeaders
        };

        if (state.token) {
            // Real Supabase RS256 JWT
            headers['Authorization'] = `Bearer ${state.token}`;
        } else if (authConfig.isDev) {
            // Local dev profile mock token fallback
            if (state.currentUser.role === 'ADMIN') {
                headers['Authorization'] = 'Bearer dev-admin';
                headers['X-Dev-Role'] = 'ADMIN';
                headers['X-Dev-User'] = state.currentUser.id;
            } else {
                headers['Authorization'] = 'Bearer dev-customer';
                headers['X-Dev-Role'] = 'CUSTOMER';
                headers['X-Dev-User'] = state.currentUser.id;
                headers['X-Dev-Priority'] = String(state.currentUser.priority || 1);
            }
        }
        return headers;
    }

    // =========================================================================
    // 1. Notification Toasts & Activity Logging
    // =========================================================================
    function showToast(message, type = 'info', icon = '🎟️') {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });

        toast.innerHTML = `
            <span class="toast-icon">${icon}</span>
            <div style="display: flex; flex-direction: column;">
                <span class="toast-msg">${message}</span>
                <span class="toast-time">${time}</span>
            </div>
        `;

        el.toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 4500);
    }

    function appendDrawerEntry(msg, type = 'INFO') {
        const entry = document.createElement('div');
        entry.className = 'drawer-entry';
        const time = new Date().toLocaleTimeString();

        let color = '#3b82f6';
        if (type === 'SEAT_RESERVED') color = '#10b981';
        if (type === 'SEAT_HELD') color = '#f59e0b';
        if (type === 'CANCEL' || type === 'EXPIRED') color = '#ef4444';

        entry.style.borderLeftColor = color;
        entry.innerHTML = `
            <div style="display: flex; justify-content: space-between; font-size: 10px; color: var(--text-muted);">
                <span>[${time}]</span>
                <span style="color: ${color}; font-weight: 700;">${type}</span>
            </div>
            <div style="color: var(--text-primary); font-size: 11px;">${msg}</div>
        `;

        el.drawerFeed.appendChild(entry);
        el.drawerFeed.scrollTop = el.drawerFeed.scrollHeight;
    }

    // =========================================================================
    // 2. Real-Time SSE Stream Integration
    // =========================================================================
    function initEventStream() {
        if (state.eventSource) {
            state.eventSource.close();
        }

        try {
            state.eventSource = new EventSource('/api/v1/events/stream');

            state.eventSource.onopen = function () {
                el.liveStatusBadge.className = 'live-status-pill';
                el.liveStatusText.textContent = 'Live Inventory Synced';
                appendDrawerEntry('Real-time SSE event stream connected.', 'CONNECTED');
            };

            state.eventSource.addEventListener('INIT', function (e) {
                appendDrawerEntry('Stream handshake initialized: ' + e.data, 'INIT');
            });

            state.eventSource.addEventListener('DOMAIN_EVENT', function (e) {
                try {
                    const event = JSON.parse(e.data);
                    handleDomainEvent(event);
                } catch (err) {
                    console.error('Error parsing domain event:', err);
                }
            });

            state.eventSource.onerror = function () {
                el.liveStatusBadge.className = 'live-status-pill';
                el.liveStatusBadge.style.borderColor = '#ef4444';
                el.liveStatusText.textContent = 'Reconnecting...';
            };
        } catch (err) {
            console.error('SSE initialization error:', err);
        }
    }

    function handleDomainEvent(event) {
        const type = event.eventType || 'SYSTEM';
        const msg = event.message || '';
        const seatNumber = event.seatNumber ? `#${event.seatNumber}` : '';
        const user = event.userId ? `(${event.userId})` : '';

        appendDrawerEntry(`${msg} ${seatNumber} ${user}`, type);

        if (type === 'SEAT_RESERVED') {
            showToast(`Confirmed booking for Seat ${seatNumber} ${user}`, 'success', '⚡');
        } else if (type === 'SEAT_HELD') {
            showToast(`Seat ${seatNumber} placed on active hold ${user}`, 'warning', '⏱️');
        } else if (type === 'VENUE_EXPANDED') {
            showToast(`Venue capacity expanded! Auto-fulfilling waiting customers...`, 'info', '🏟️');
        } else if (type === 'SEAT_EXPIRED' || type === 'RESERVATION_CANCELLED') {
            showToast(`Seat ${seatNumber} released back to inventory`, 'info', '🔄');
        }

        fetchSystemStatus();
        fetchSeats();
        fetchWaitlist();
        fetchMyTickets();
    }

    // =========================================================================
    // 3. Telemetry & Seat Grid Rendering
    // =========================================================================
    async function fetchSystemStatus() {
        try {
            const res = await fetch('/api/v1/seats/availability');
            if (!res.ok) return;
            const json = await res.json();
            const data = json.data;
            state.systemStatus = data;

            el.statAvailableCount.textContent = data.availableSeats;
            el.statReservedCount.textContent = data.reservedSeats;
            el.statHeldCount.textContent = data.heldSeats;
            el.statWaitlistCount.textContent = data.waitlistCount;

            const total = data.totalSeats || 1;
            const reservedPct = (data.reservedSeats / total) * 100;
            const heldPct = (data.heldSeats / total) * 100;
            const availablePct = (data.availableSeats / total) * 100;

            el.barReserved.style.width = `${reservedPct}%`;
            el.barHeld.style.width = `${heldPct}%`;
            el.barAvailable.style.width = `${availablePct}%`;

            const bookedPct = Math.round(((data.reservedSeats + data.heldSeats) / total) * 100);
            el.statOccupancyPct.textContent = `${bookedPct}% Booked (${data.totalSeats} Total)`;
        } catch (err) {
            console.error('Failed to fetch system status:', err);
        }
    }

    async function fetchSeats() {
        try {
            const res = await fetch('/api/v1/seats', { headers: getAuthHeaders() });
            if (!res.ok) {
                if (res.status === 401) {
                    el.seatGrid.innerHTML = `
                        <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: var(--text-secondary);">
                            <p>🔒 Please sign in above to view seat inventory &amp; book tickets.</p>
                            <button class="btn-primary" style="width: auto; margin: 12px auto 0;" onclick="document.getElementById('btnUserAccount').click()">
                                Sign In / Sign Up
                            </button>
                        </div>
                    `;
                }
                return;
            }
            const json = await res.json();
            const seats = json.data || [];
            state.seats = seats;
            renderSeatGrid(seats);
        } catch (err) {
            console.error('Failed to fetch seats:', err);
        }
    }

    function renderSeatGrid(seats) {
        el.seatGrid.innerHTML = '';

        if (!seats || seats.length === 0) {
            el.seatGrid.innerHTML = `
                <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: var(--text-secondary);">
                    <p>🏟️ No seats initialized yet.</p>
                    <p style="font-size: 12px; margin-top: 6px;">Sign in as Admin to initialize venue capacity.</p>
                </div>
            `;
            return;
        }

        seats.forEach(seat => {
            const node = document.createElement('div');
            const tierInfo = getTierInfo(seat.seatNumber, seat.tier);
            const isSelected = state.selectedSeat && state.selectedSeat.seatNumber === seat.seatNumber;

            node.className = `seat-node ${seat.status} ${tierInfo.className} ${isSelected ? 'selected' : ''}`;
            node.id = `seat-${seat.seatNumber}`;

            node.innerHTML = `
                <span class="seat-num">#${seat.seatNumber}</span>
                <span class="seat-price-tag">${tierInfo.code}</span>
            `;

            node.title = `Seat #${seat.seatNumber} (${tierInfo.name} - ${tierInfo.price}) - ${seat.status}${seat.occupantUserId ? ' [' + seat.occupantUserId + ']' : ''}`;

            node.addEventListener('click', () => {
                selectSeat(seat, tierInfo);
            });

            el.seatGrid.appendChild(node);
        });
    }

    function selectSeat(seat, tierInfo) {
        state.selectedSeat = seat;
        document.querySelectorAll('.seat-node').forEach(n => n.classList.remove('selected'));
        const node = document.getElementById(`seat-${seat.seatNumber}`);
        if (node) node.classList.add('selected');

        el.summarySeatNum.textContent = `Seat #${seat.seatNumber}`;
        el.summarySeatPrice.textContent = tierInfo.price;
        el.summarySeatStatus.textContent = seat.status;
        el.summarySeatStatus.style.color = seat.status === 'AVAILABLE' ? '#10b981' : (seat.status === 'HELD' ? '#f59e0b' : '#64748b');
        el.summarySeatTier.textContent = tierInfo.name;
        el.summaryFanName.textContent = state.currentUser.name;

        el.selectedTierBadge.style.display = 'inline-flex';
        el.selectedTierBadge.className = `tier-badge-pill ${tierInfo.className}`;
        el.selectedTierBadge.textContent = tierInfo.name;

        if (seat.status === 'RESERVED') {
            el.btnReserveSeat.disabled = true;
            el.btnReserveSeat.style.opacity = '0.5';
            el.btnHoldSeat.disabled = true;
            el.btnHoldSeat.style.opacity = '0.5';
        } else {
            el.btnReserveSeat.disabled = false;
            el.btnReserveSeat.style.opacity = '1';
            el.btnHoldSeat.disabled = false;
            el.btnHoldSeat.style.opacity = '1';
        }
    }

    // =========================================================================
    // 4. Customer Booking & Hold Actions
    // =========================================================================
    async function reserveSeat() {
        const userId = state.currentUser.id || state.currentUser.email || 'usr_fan';
        const priority = state.currentUser.priority || 1;

        try {
            el.btnReserveSeat.innerHTML = '<span>⏳ Reserving...</span>';
            const res = await fetch('/api/v1/reservations', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ userId, priority })
            });
            const json = await res.json();
            el.btnReserveSeat.innerHTML = '<span>⚡ Confirm &amp; Reserve Seat</span>';

            if (res.status === 201) {
                showToast(`🎉 Congratulations! Seat #${json.data.seatNumber} booked successfully!`, 'success', '✅');
                clearHoldTimer();
            } else if (res.status === 202) {
                showToast(`⏳ Venue sold out! Added ${userId} to Priority Waitlist.`, 'warning', '⏳');
            } else if (res.status === 401) {
                showToast('🔒 Session expired. Please sign in to book.', 'warning');
                el.authModal.classList.add('active');
            } else {
                showToast(`⚠️ ${json.detail || json.message || 'Booking unsuccessful'}`, 'warning');
            }

            fetchSystemStatus();
            fetchSeats();
            fetchWaitlist();
            fetchMyTickets();
        } catch (err) {
            console.error('Reservation error:', err);
            el.btnReserveSeat.innerHTML = '<span>⚡ Confirm &amp; Reserve Seat</span>';
        }
    }

    async function holdSeat() {
        const userId = state.currentUser.id || state.currentUser.email || 'usr_fan';
        const priority = state.currentUser.priority || 1;
        const ttl = 60;

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
            el.btnHoldSeat.innerHTML = '<span>⏳ Placing Hold...</span>';
            const res = await fetch('/graphql', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ query })
            });
            const json = await res.json();
            el.btnHoldSeat.innerHTML = '<span>⏱️ Place 60s Hold</span>';

            if (json.data && json.data.holdSeat) {
                const seatNum = json.data.holdSeat.seatNumber;
                showToast(`⏱️ Seat #${seatNum} is held for 60 seconds!`, 'warning', '⏱️');
                startHoldTimer(60);
            } else {
                showToast('⚠️ Unable to hold seat or venue sold out.', 'warning');
            }

            fetchSystemStatus();
            fetchSeats();
            fetchWaitlist();
        } catch (err) {
            console.error('Hold error:', err);
            el.btnHoldSeat.innerHTML = '<span>⏱️ Place 60s Hold</span>';
        }
    }

    function startHoldTimer(seconds) {
        clearHoldTimer();
        state.holdSecondsRemaining = seconds;
        el.holdTimerBox.classList.add('active');
        updateHoldTimerDisplay();

        state.holdInterval = setInterval(() => {
            state.holdSecondsRemaining--;
            if (state.holdSecondsRemaining <= 0) {
                clearHoldTimer();
                showToast('⏱️ Seat hold expired and returned to inventory.', 'info', '🔄');
                fetchSystemStatus();
                fetchSeats();
            } else {
                updateHoldTimerDisplay();
            }
        }, 1000);
    }

    function updateHoldTimerDisplay() {
        const mins = Math.floor(state.holdSecondsRemaining / 60);
        const secs = state.holdSecondsRemaining % 60;
        el.holdCountdownVal.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
    }

    function clearHoldTimer() {
        if (state.holdInterval) {
            clearInterval(state.holdInterval);
            state.holdInterval = null;
        }
        el.holdTimerBox.classList.remove('active');
    }

    // =========================================================================
    // 5. "My Digital Passes" Render
    // =========================================================================
    async function fetchMyTickets() {
        if (state.currentUser.role === 'ADMIN') {
            el.myTicketsContainer.innerHTML = `
                <p style="color: var(--text-muted); font-size: 13px;">
                    👑 Logged in as Administrator. Switch to Customer mode to view personal digital tickets.
                </p>
            `;
            return;
        }

        try {
            const res = await fetch('/api/v1/reservations', { headers: getAuthHeaders() });
            if (!res.ok) return;
            const json = await res.json();
            const allReservations = json.data || [];
            
            const myId = state.currentUser.id || state.currentUser.email;
            const myReservations = allReservations.filter(r => r.userId === myId);

            if (myReservations.length === 0) {
                el.myTicketsContainer.innerHTML = `
                    <p style="color: var(--text-muted); font-size: 13px;">
                        No tickets currently reserved for <strong>${state.currentUser.name}</strong>. Select a seat above to book!
                    </p>
                `;
                return;
            }

            el.myTicketsContainer.innerHTML = '';
            myReservations.forEach(ticket => {
                const tierInfo = getTierInfo(ticket.seatNumber, ticket.tier);
                const card = document.createElement('div');
                card.className = 'ticket-pass';

                card.innerHTML = `
                    <div class="ticket-pass-header">
                        <span class="ticket-event-name">Cyber Symphony 2026</span>
                        <span class="ticket-tier-tag">${tierInfo.code} PASS</span>
                    </div>
                    <div class="ticket-pass-body">
                        <div>
                            <div class="ticket-seat-highlight">SEAT #${ticket.seatNumber}</div>
                            <div class="ticket-holder">Fan: ${state.currentUser.name}</div>
                            <div style="font-size: 12px; color: #34d399; margin-top: 4px;">● Confirmed Admission</div>
                        </div>
                        <div class="ticket-qr-mockup">
                            <div style="font-family: monospace; font-size: 8px; line-height: 1; text-align: center; color: #000;">
                                ■■■□■■<br>■□□■□■<br>■■■□■■<br>□□■■□□<br>■■□■■■
                            </div>
                        </div>
                    </div>
                    <div class="ticket-pass-footer">
                        <span class="ticket-code">CONF-${ticket.seatNumber}099-TF</span>
                        <button class="btn-danger" style="padding: 6px 12px; font-size: 11px;" onclick="window.cancelTicket(${ticket.seatNumber})">
                            Cancel Pass
                        </button>
                    </div>
                `;

                el.myTicketsContainer.appendChild(card);
            });
        } catch (err) {
            console.error('Error fetching user tickets:', err);
        }
    }

    window.cancelTicket = async function (seatNumber) {
        const userId = state.currentUser.id || state.currentUser.email;
        try {
            const res = await fetch(`/api/v1/reservations/${seatNumber}?userId=${encodeURIComponent(userId)}`, {
                method: 'DELETE',
                headers: getAuthHeaders()
            });
            if (res.ok) {
                showToast(`Ticket for Seat #${seatNumber} cancelled and refunded.`, 'info', '🗑️');
                fetchSystemStatus();
                fetchSeats();
                fetchMyTickets();
            }
        } catch (err) {
            console.error('Cancellation error:', err);
        }
    };

    // =========================================================================
    // 6. Admin Command Operations
    // =========================================================================
    async function adminInitialize() {
        const count = parseInt(el.adminInitCount.value, 10) || 50;
        try {
            el.btnAdminInitVenue.innerHTML = '<span>Resetting...</span>';
            const res = await fetch('/api/v1/seats/initialize', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ seatCount: count })
            });
            el.btnAdminInitVenue.innerHTML = '<span>Reset Venue</span>';

            if (res.ok) {
                showToast(`👑 Venue initialized with ${count} fresh seats!`, 'success', '🏟️');
                fetchSystemStatus();
                fetchSeats();
                fetchWaitlist();
                fetchMyTickets();
            } else {
                showToast('Admin permission required to initialize venue.', 'warning');
            }
        } catch (err) {
            console.error('Admin initialize error:', err);
            el.btnAdminInitVenue.innerHTML = '<span>Reset Venue</span>';
        }
    }

    async function adminExpand() {
        const count = parseInt(el.adminExpandCount.value, 10) || 10;
        try {
            el.btnAdminExpandVenue.innerHTML = '<span>Adding...</span>';
            const res = await fetch('/api/v1/seats/expand', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ additionalCount: count })
            });
            el.btnAdminExpandVenue.innerHTML = '<span>+ Add Seats</span>';

            if (res.ok) {
                showToast(`👑 Capacity expanded by +${count} seats! Queue fulfilled.`, 'success', '➕');
                fetchSystemStatus();
                fetchSeats();
                fetchWaitlist();
                fetchMyTickets();
            }
        } catch (err) {
            console.error('Admin expand error:', err);
            el.btnAdminExpandVenue.innerHTML = '<span>+ Add Seats</span>';
        }
    }

    async function adminReleaseRange() {
        const from = el.adminReleaseFrom.value.trim();
        const to = el.adminReleaseTo.value.trim();
        if (!from || !to) {
            showToast('Please specify both From and To user handles.', 'warning');
            return;
        }

        try {
            const res = await fetch('/api/v1/reservations/release-range', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ fromUserId: from, toUserId: to })
            });
            const json = await res.json();
            if (res.ok) {
                const count = json.data ? json.data.length : 0;
                showToast(`👑 Batch released ${count} seats for range [${from}, ${to}].`, 'info', '✂️');
                fetchSystemStatus();
                fetchSeats();
                fetchWaitlist();
                fetchMyTickets();
            }
        } catch (err) {
            console.error('Admin release range error:', err);
        }
    }

    async function simulateFlashBurst() {
        el.burstStatusMsg.textContent = 'Firing 10 concurrent requests across virtual threads...';
        el.btnAdminBurstTest.disabled = true;

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
        el.burstStatusMsg.textContent = '✅ Burst completed! Inventory updated via Redis & Virtual Threads.';
        el.btnAdminBurstTest.disabled = false;
        fetchSystemStatus();
        fetchSeats();
        fetchWaitlist();
    }

    async function fetchWaitlist() {
        try {
            const res = await fetch('/api/v1/waitlist', { headers: getAuthHeaders() });
            if (!res.ok) return;
            const json = await res.json();
            const waitlist = json.data || [];
            state.waitlist = waitlist;
            renderAdminWaitlist(waitlist);
        } catch (err) {
            console.error('Failed to fetch waitlist:', err);
        }
    }

    function renderAdminWaitlist(waitlist) {
        if (!waitlist || waitlist.length === 0) {
            el.adminWaitlistTableContainer.innerHTML = '<p style="color: var(--text-muted); font-size: 13px;">No users currently on waitlist.</p>';
            return;
        }

        let html = `
            <table class="waitlist-table">
                <thead>
                    <tr>
                        <th>Queue Pos</th>
                        <th>User Handle</th>
                        <th>Priority Tier</th>
                        <th>Operations</th>
                    </tr>
                </thead>
                <tbody>
        `;

        waitlist.forEach(entry => {
            html += `
                <tr>
                    <td style="font-weight: 700; color: #60a5fa;">#${entry.queuePosition}</td>
                    <td><strong>${entry.userId}</strong></td>
                    <td><span class="tier-badge-pill vip" style="padding: 2px 8px; font-size: 11px;">Tier ${entry.priority}</span></td>
                    <td>
                        <button class="preset-btn" style="padding: 4px 8px; font-size: 11px;" onclick="window.promoteWaitlistUser('${entry.userId}', ${entry.priority + 1})">Promote</button>
                        <button class="btn-danger" style="padding: 4px 8px; font-size: 11px;" onclick="window.removeWaitlistUser('${entry.userId}')">Remove</button>
                    </td>
                </tr>
            `;
        });

        html += '</tbody></table>';
        el.adminWaitlistTableContainer.innerHTML = html;
    }

    window.promoteWaitlistUser = async function (userId, newPriority) {
        try {
            const res = await fetch(`/api/v1/waitlist/${userId}`, {
                method: 'PATCH',
                headers: getAuthHeaders(),
                body: JSON.stringify({ newPriority: Math.min(5, newPriority) })
            });
            if (res.ok) {
                showToast(`Promoted ${userId} to higher queue priority`, 'info');
                fetchWaitlist();
            }
        } catch (err) {
            console.error(err);
        }
    };

    window.removeWaitlistUser = async function (userId) {
        try {
            const res = await fetch(`/api/v1/waitlist/${userId}`, {
                method: 'DELETE',
                headers: getAuthHeaders()
            });
            if (res.ok) {
                showToast(`Removed ${userId} from queue`, 'info');
                fetchWaitlist();
                fetchSystemStatus();
            }
        } catch (err) {
            console.error(err);
        }
    };

    // =========================================================================
    // 7. Supabase & Client Authentication Logic
    // =========================================================================
    async function fetchAuthConfig() {
        try {
            const res = await fetch('/api/v1/auth/config');
            if (res.ok) {
                const json = await res.json();
                if (json.data) {
                    authConfig = json.data;
                }
            }
        } catch (err) {
            console.warn('Unable to load auth config:', err);
        }
    }

    async function handleSignIn(email, password) {
        showAuthAlert('Signing in...', 'info');
        
        // If in Dev mode or mock profile
        if (authConfig.isDev) {
            const isAdm = email.includes('admin') || password.includes('admin');
            const role = isAdm ? 'ADMIN' : 'CUSTOMER';
            const name = email.split('@')[0].toUpperCase();
            state.currentUser = { id: 'usr_' + email.split('@')[0], name, email, role, priority: 3 };
            state.token = null;
            saveUserState();
            updateUserProfileUI();
            hideAuthModal();
            showToast(`Signed in as ${name}`, 'success', '👤');
            fetchSeats();
            fetchMyTickets();
            return;
        }

        // Production / Staging Supabase OAuth2 Token Request
        try {
            const endpoint = `${authConfig.supabaseUrl}/auth/v1/token?grant_type=password`;
            const res = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'apikey': authConfig.supabaseAnonKey
                },
                body: JSON.stringify({ email, password })
            });

            const json = await res.json();
            if (res.ok && json.access_token) {
                state.token = json.access_token;
                const user = json.user || {};
                const userMeta = user.user_metadata || {};
                const appMeta = user.app_metadata || {};

                let role = 'CUSTOMER';
                if (appMeta.role === 'ROLE_ADMIN' || appMeta.role === 'admin' || userMeta.role === 'admin') {
                    role = 'ADMIN';
                }

                state.currentUser = {
                    id: user.id || email.split('@')[0],
                    name: userMeta.name || email.split('@')[0],
                    email: user.email || email,
                    role: role,
                    priority: parseInt(userMeta.priority_tier, 10) || 1
                };

                saveUserState();
                updateUserProfileUI();
                hideAuthModal();
                showToast(`Welcome back, ${state.currentUser.name}!`, 'success', '🎟️');
                fetchSeats();
                fetchMyTickets();
            } else {
                showAuthAlert(json.error_description || json.msg || 'Invalid email or password.', 'error');
            }
        } catch (err) {
            console.error('Supabase Auth error:', err);
            showAuthAlert('Unable to reach authentication server.', 'error');
        }
    }

    async function handleSignUp(name, email, password, priority) {
        showAuthAlert('Creating account...', 'info');

        if (authConfig.isDev) {
            state.currentUser = { id: 'usr_' + email.split('@')[0], name, email, role: 'CUSTOMER', priority };
            state.token = null;
            saveUserState();
            updateUserProfileUI();
            hideAuthModal();
            showToast(`Account created for ${name}!`, 'success', '✅');
            fetchSeats();
            fetchMyTickets();
            return;
        }

        try {
            const endpoint = `${authConfig.supabaseUrl}/auth/v1/signup`;
            const res = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'apikey': authConfig.supabaseAnonKey
                },
                body: JSON.stringify({
                    email,
                    password,
                    data: {
                        name: name,
                        priority_tier: priority,
                        role: 'ROLE_CUSTOMER'
                    }
                })
            });

            const json = await res.json();
            if (res.ok) {
                if (json.access_token) {
                    state.token = json.access_token;
                    state.currentUser = {
                        id: json.user.id,
                        name,
                        email,
                        role: 'CUSTOMER',
                        priority
                    };
                    saveUserState();
                    updateUserProfileUI();
                    hideAuthModal();
                    showToast(`Account created & signed in as ${name}!`, 'success', '🎉');
                    fetchSeats();
                    fetchMyTickets();
                } else {
                    showAuthAlert('Sign up successful! Please check your email to confirm registration or sign in.', 'success');
                }
            } else {
                showAuthAlert(json.error_description || json.msg || 'Registration failed.', 'error');
            }
        } catch (err) {
            console.error('Sign up error:', err);
            showAuthAlert('Authentication error during registration.', 'error');
        }
    }

    function showAuthAlert(msg, type) {
        el.authAlertMsg.style.display = 'block';
        el.authAlertMsg.textContent = msg;
        el.authAlertMsg.style.background = type === 'error' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(59, 130, 246, 0.15)';
        el.authAlertMsg.style.color = type === 'error' ? '#ef4444' : '#60a5fa';
        el.authAlertMsg.style.border = `1px solid ${type === 'error' ? 'rgba(239, 68, 68, 0.3)' : 'rgba(59, 130, 246, 0.3)'}`;
    }

    function hideAuthModal() {
        el.authModal.classList.remove('active');
        el.authAlertMsg.style.display = 'none';
    }

    function saveUserState() {
        localStorage.setItem('tf_user', JSON.stringify(state.currentUser));
        if (state.token) {
            localStorage.setItem('tf_token', state.token);
        } else {
            localStorage.removeItem('tf_token');
        }
    }

    function loadSavedUser() {
        try {
            const savedUser = localStorage.getItem('tf_user');
            const savedToken = localStorage.getItem('tf_token');
            if (savedUser) state.currentUser = JSON.parse(savedUser);
            if (savedToken) state.token = savedToken;
        } catch (e) {
            console.error('Error loading saved user:', e);
        }
        updateUserProfileUI();
    }

    function updateUserProfileUI() {
        if (state.currentUser.role === 'ADMIN') {
            el.navUserAvatar.textContent = '👑';
            el.navUserAvatar.className = 'user-avatar admin-avatar';
            el.navUserName.textContent = state.currentUser.name || 'Admin';
            el.navUserRole.textContent = '👑 Venue Operations';
            el.summaryFanName.textContent = 'Venue Admin';
            el.tabAdminConsole.style.display = 'inline-flex';
        } else {
            el.navUserAvatar.textContent = (state.currentUser.name || 'A').charAt(0).toUpperCase();
            el.navUserAvatar.className = 'user-avatar';
            el.navUserName.textContent = state.currentUser.name;
            const prio = state.currentUser.priority || 1;
            const tierText = prio === 3 ? '⭐ VIP Member (Tier 3)' : (prio === 2 ? '💎 Presale Pass (Tier 2)' : '🟢 Standard Fan (Tier 1)');
            el.navUserRole.textContent = tierText;
            el.summaryFanName.textContent = state.currentUser.name;
            el.userWaitlistTier.textContent = `Tier ${prio}`;
        }
    }

    function switchView(viewName) {
        state.activeView = viewName;
        if (viewName === 'ADMIN') {
            el.tabCustomerPortal.classList.remove('active');
            el.tabAdminConsole.classList.add('active');
            el.customerView.classList.remove('active');
            el.adminView.classList.add('active');
        } else {
            el.tabAdminConsole.classList.remove('active');
            el.tabCustomerPortal.classList.add('active');
            el.adminView.classList.remove('active');
            el.customerView.classList.add('active');
        }
    }

    window.demoSignIn = function (email, name, role, priority) {
        state.currentUser = { id: 'usr_' + email.split('@')[0], name, email, role, priority };
        state.token = null;
        saveUserState();
        updateUserProfileUI();
        hideAuthModal();
        showToast(`Signed in as ${name}`, 'success', '👤');
        fetchSeats();
        fetchMyTickets();
    };

    // =========================================================================
    // 8. Event Listeners & Bootstrapping
    // =========================================================================
    function initEventListeners() {
        // Navigation View Tabs
        el.tabCustomerPortal.addEventListener('click', () => switchView('CUSTOMER'));
        el.tabAdminConsole.addEventListener('click', () => {
            if (state.currentUser.role !== 'ADMIN') {
                state.currentUser.role = 'ADMIN';
                updateUserProfileUI();
                showToast('Switched to Venue Administrator mode', 'info', '👑');
            }
            switchView('ADMIN');
        });

        // Auth Modal Handlers
        el.btnUserAccount.addEventListener('click', () => {
            el.authModal.classList.add('active');
        });

        el.btnCloseAuthModal.addEventListener('click', hideAuthModal);
        el.authModal.addEventListener('click', (e) => {
            if (e.target === el.authModal) hideAuthModal();
        });

        el.tabSignIn.addEventListener('click', () => {
            el.tabSignIn.classList.add('active');
            el.tabSignUp.classList.remove('active');
            el.tabAdminAuth.classList.remove('active');
            el.signInForm.style.display = 'block';
            el.signUpForm.style.display = 'none';
            el.adminAuthForm.style.display = 'none';
            el.authAlertMsg.style.display = 'none';
        });

        el.tabSignUp.addEventListener('click', () => {
            el.tabSignUp.classList.add('active');
            el.tabSignIn.classList.remove('active');
            el.tabAdminAuth.classList.remove('active');
            el.signUpForm.style.display = 'block';
            el.signInForm.style.display = 'none';
            el.adminAuthForm.style.display = 'none';
            el.authAlertMsg.style.display = 'none';
        });

        el.tabAdminAuth.addEventListener('click', () => {
            el.tabAdminAuth.classList.add('active');
            el.tabSignIn.classList.remove('active');
            el.tabSignUp.classList.remove('active');
            el.adminAuthForm.style.display = 'block';
            el.signInForm.style.display = 'none';
            el.signUpForm.style.display = 'none';
            el.authAlertMsg.style.display = 'none';
        });

        el.btnSubmitSignIn.addEventListener('click', () => {
            const email = el.loginEmail.value.trim();
            const pass = el.loginPassword.value;
            if (!email || !pass) {
                showAuthAlert('Please enter email and password.', 'error');
                return;
            }
            handleSignIn(email, pass);
        });

        el.btnSubmitSignUp.addEventListener('click', () => {
            const name = el.signupName.value.trim();
            const email = el.signupEmail.value.trim();
            const pass = el.signupPassword.value;
            const priority = parseInt(el.signupPriority.value, 10) || 1;
            if (!name || !email || !pass) {
                showAuthAlert('Please complete all registration fields.', 'error');
                return;
            }
            handleSignUp(name, email, pass, priority);
        });

        el.btnSubmitAdminAuth.addEventListener('click', () => {
            const email = el.adminAuthEmail.value.trim();
            const pass = el.adminAuthPassword.value;
            handleSignIn(email, pass);
            switchView('ADMIN');
        });

        // Customer Booking Buttons
        el.btnSyncSeats.addEventListener('click', () => {
            fetchSystemStatus();
            fetchSeats();
            showToast('Arena seat map synchronized', 'info', '🔄');
        });

        el.btnReserveSeat.addEventListener('click', reserveSeat);
        el.btnHoldSeat.addEventListener('click', holdSeat);
        el.btnJoinWaitlistDirect.addEventListener('click', reserveSeat);

        // Admin Buttons
        el.btnAdminInitVenue.addEventListener('click', adminInitialize);
        el.btnAdminExpandVenue.addEventListener('click', adminExpand);
        el.btnAdminReleaseRange.addEventListener('click', adminReleaseRange);
        el.btnAdminBurstTest.addEventListener('click', simulateFlashBurst);
        el.btnAdminRefreshWaitlist.addEventListener('click', fetchWaitlist);

        // Activity Drawer Handlers
        el.btnOpenDrawer.addEventListener('click', () => {
            el.drawerBackdrop.classList.add('active');
        });

        el.btnCloseDrawer.addEventListener('click', () => {
            el.drawerBackdrop.classList.remove('active');
        });

        el.drawerBackdrop.addEventListener('click', (e) => {
            if (e.target === el.drawerBackdrop) el.drawerBackdrop.classList.remove('active');
        });

        el.btnClearActivityFeed.addEventListener('click', () => {
            el.drawerFeed.innerHTML = '';
        });
    }

    // App Initialization
    document.addEventListener('DOMContentLoaded', async () => {
        await fetchAuthConfig();
        loadSavedUser();
        initEventListeners();
        initEventStream();
        fetchSystemStatus();
        fetchSeats();
        fetchWaitlist();
        fetchMyTickets();
    });

})();
