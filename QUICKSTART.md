# 🚀 Quick Start Guide - Eternity Solver Monitoring

## One-Command Start

```bash
./start-monitoring.sh
```

This will:
1. ✅ Check prerequisites (Java, Maven, Node.js)
2. 🔨 Build the backend
3. 📦 Install frontend dependencies (if needed)
4. 🚀 Start backend on port 8080
5. 🎨 Start frontend on port 3000

## Access the Dashboard

Open your browser to: **http://localhost:3000**

## Stop the System

```bash
./stop-monitoring.sh
```

## Manual Start (Alternative)

### Terminal 1: Backend
```bash
mvn spring-boot:run
```

### Terminal 2: Frontend
```bash
cd frontend
npm run dev
```

### Terminal 3: Solver (Optional)
```bash
mvn exec:java -Dexec.mainClass="MainParallel"
```

## What You'll See

### 📊 Dashboard Features:
1. **Global Stats** - Overview of all configurations
2. **Metrics Legend** - Click "Learn More" to understand metrics
3. **Configuration Table** - Real-time updates for all configs
4. **Historical Charts** - Click "View Details" on any config

### 🎯 Key Metrics:
- **Physical %** (Blue): Actual pieces placed
- **Search Space** (Green/Red): Solver's progress estimation
- **Δ Delta**: Difference between the two
  - High delta (red) = likely impossible configuration
  - Low delta (gray) = healthy progress

## Troubleshooting

### Port Already in Use
```bash
# Kill process on port 8080
lsof -i :8080
kill -9 <PID>

# Or use different port in application.properties
server.port=8081
```

### No Data Showing
1. Check solver is running and creating files in `saves/`
2. Verify backend logs: `tail -f logs/backend.log`
3. Check frontend console (F12 in browser)

### WebSocket Not Connecting
1. Ensure backend is running: `curl http://localhost:8080/api/health`
2. Check browser console for errors
3. Try hard refresh: Ctrl+Shift+R (Chrome)

## Next Steps

- Read full documentation: [`MONITORING.md`](./MONITORING.md)
- Explore API endpoints: `http://localhost:8080/api/health`
- View logs: `logs/backend.log` and `logs/frontend.log`

---

**Need Help?** Check the full [MONITORING.md](./MONITORING.md) documentation.
