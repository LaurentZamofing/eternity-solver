import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { ConfigMetrics, GlobalStats } from '../types/metrics';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

export type MetricsCallback = (metrics: ConfigMetrics) => void;
export type StatsCallback = (stats: GlobalStats) => void;
export type ConnectionCallback = () => void;
export type ErrorCallback = (error: any) => void;

/**
 * WebSocket service for real-time updates using STOMP over SockJS
 */
export class WebSocketService {
  private client: Client | null = null;
  private connected: boolean = false;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 10;

  // Callbacks
  private metricsCallbacks: Set<MetricsCallback> = new Set();
  private statsCallbacks: Set<StatsCallback> = new Set();
  private connectCallbacks: Set<ConnectionCallback> = new Set();
  private disconnectCallbacks: Set<ConnectionCallback> = new Set();
  private errorCallbacks: Set<ErrorCallback> = new Set();

  constructor() {
    this.client = null;
  }

  /**
   * Connect to WebSocket server
   */
  connect(): void {
    if (this.client && this.connected) {
      console.log('WebSocket already connected');
      return;
    }

    console.log('Connecting to WebSocket...');

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as any,
      debug: (str) => {
        console.log('[STOMP Debug]', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = () => {
      console.log('✅ WebSocket connected');
      this.connected = true;
      this.reconnectAttempts = 0;

      // Subscribe to topics
      this.subscribeToTopics();

      // Notify connection callbacks
      this.connectCallbacks.forEach((callback) => callback());
    };

    this.client.onDisconnect = () => {
      console.log('WebSocket disconnected');
      this.connected = false;

      // Notify disconnect callbacks
      this.disconnectCallbacks.forEach((callback) => callback());
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      this.errorCallbacks.forEach((callback) => callback(frame));
    };

    this.client.onWebSocketError = (event) => {
      console.error('WebSocket error:', event);
      this.errorCallbacks.forEach((callback) => callback(event));

      // Attempt reconnection
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++;
        console.log(`Reconnection attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
      }
    };

    this.client.activate();
  }

  /**
   * Subscribe to STOMP topics
   */
  private subscribeToTopics(): void {
    if (!this.client || !this.connected) {
      console.error('Cannot subscribe: not connected');
      return;
    }

    // Subscribe to metrics updates
    this.client.subscribe('/topic/metrics', (message: IMessage) => {
      try {
        const metrics: ConfigMetrics = JSON.parse(message.body);
        console.log('Received metrics update:', metrics.configName);
        this.metricsCallbacks.forEach((callback) => callback(metrics));
      } catch (error) {
        console.error('Failed to parse metrics message:', error);
      }
    });

    // Subscribe to stats updates
    this.client.subscribe('/topic/stats', (message: IMessage) => {
      try {
        const stats: GlobalStats = JSON.parse(message.body);
        console.log('Received stats update');
        this.statsCallbacks.forEach((callback) => callback(stats));
      } catch (error) {
        console.error('Failed to parse stats message:', error);
      }
    });

    console.log('Subscribed to /topic/metrics and /topic/stats');
  }

  /**
   * Disconnect from WebSocket
   */
  disconnect(): void {
    if (this.client) {
      console.log('Disconnecting WebSocket...');
      this.client.deactivate();
      this.client = null;
      this.connected = false;
    }
  }

  /**
   * Send a message to the server
   */
  send(destination: string, body: any): void {
    if (!this.client || !this.connected) {
      console.error('Cannot send message: not connected');
      return;
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }

  /**
   * Request config details (example bidirectional communication)
   */
  requestConfig(configName: string): void {
    this.send('/app/getConfig', { configName });
  }

  /**
   * Send ping for keep-alive
   */
  ping(): void {
    this.send('/app/ping', { type: 'ping' });
  }

  /**
   * Register callback for metrics updates
   */
  onMetricsUpdate(callback: MetricsCallback): () => void {
    this.metricsCallbacks.add(callback);
    // Return unsubscribe function
    return () => {
      this.metricsCallbacks.delete(callback);
    };
  }

  /**
   * Register callback for stats updates
   */
  onStatsUpdate(callback: StatsCallback): () => void {
    this.statsCallbacks.add(callback);
    return () => {
      this.statsCallbacks.delete(callback);
    };
  }

  /**
   * Register callback for connection
   */
  onConnect(callback: ConnectionCallback): () => void {
    this.connectCallbacks.add(callback);
    return () => {
      this.connectCallbacks.delete(callback);
    };
  }

  /**
   * Register callback for disconnection
   */
  onDisconnect(callback: ConnectionCallback): () => void {
    this.disconnectCallbacks.add(callback);
    return () => {
      this.disconnectCallbacks.delete(callback);
    };
  }

  /**
   * Register callback for errors
   */
  onError(callback: ErrorCallback): () => void {
    this.errorCallbacks.add(callback);
    return () => {
      this.errorCallbacks.delete(callback);
    };
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.connected;
  }
}

// Export singleton instance
export const websocketService = new WebSocketService();

export default websocketService;
