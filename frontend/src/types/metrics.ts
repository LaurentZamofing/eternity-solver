/**
 * TypeScript interfaces matching the Java backend models
 */

export interface PlacementInfo {
  row: number;
  col: number;
  pieceId: number;
  rotation: number;
  sequenceNumber: number; // Order in which this piece was placed (1-based)
}

export interface ConfigMetrics {
  configName: string;
  timestamp: number;
  lastUpdate: string; // LocalDateTime as ISO string
  lastSaveDate: string; // Formatted last save date (e.g., "2025-12-03 08:14:05")
  lastSaveRelative: string; // Relative time since last save (e.g., "2m ago")
  depth: number;
  bestDepthEver: number; // Highest depth ever reached (from best_*.txt files)
  progressPercentage: number; // Search space estimation from solver
  physicalProgressPercentage: number; // Actual pieces placed ratio
  totalComputeTimeMs: number;
  computeTimeFormatted: string;
  rows: number;
  cols: number;
  totalPieces: number;
  running: boolean;
  solved: boolean;
  threadId?: string;
  status: 'running' | 'idle' | 'solved' | 'stuck' | 'active' | 'milestone';
  piecesPerSecond: number;
  estimatedTimeRemainingMs: number;
  boardState?: string[][]; // [row][col] = "pieceId_rotation" or null
  placementOrder?: PlacementInfo[]; // Chronological order of piece placements
  positionToSequence?: { [key: string]: number }; // Map from "row,col" to sequence number
}

export interface HistoricalMetrics {
  id: number;
  configName: string;
  timestamp: string; // LocalDateTime as ISO string
  depth: number;
  progressPercentage: number;
  totalComputeTimeMs: number;
  status: string;
  threadId?: string;
  piecesPerSecond: number;
}

export interface GlobalStats {
  totalConfigs: number;
  runningConfigs: number;
  solvedConfigs: number;
  idleConfigs: number;
  stuckConfigs: number;
  totalComputeTimeMs: number;
  totalComputeTimeFormatted: string;
  bestProgressPercentage: number;
  bestProgressConfigName: string;
  averageProgressPercentage: number;
  maxDepth: number;
  maxDepthConfigName: string;
  fastestPiecesPerSecond: number;
  fastestConfigName: string;
  topConfigs: ConfigMetrics[];
  stuckConfigsList: ConfigMetrics[];
}

export interface ThreadStatus {
  threadId: string;
  threadName: string;
  currentConfigName: string;
  startTime: string;
  lastActivity: string;
  status: 'running' | 'idle' | 'rotating' | 'finished';
  tasksCompleted: number;
  currentDepth: number;
}

export type SortField = 'progress' | 'depth' | 'bestDepthEver' | 'time' | 'name';
export type SortOrder = 'asc' | 'desc';
export type StatusFilter = 'all' | 'running' | 'idle' | 'solved' | 'stuck';

export interface PieceDefinition {
  pieceId: number;
  north: number;   // Top edge pattern
  east: number;    // Right edge pattern
  south: number;   // Bottom edge pattern
  west: number;    // Left edge pattern
}

export interface PieceOption {
  pieceId: number;
  rotation: number; // 0-3 (number of 90° clockwise rotations)
  edges: number[]; // [N, E, S, W] after rotation
  valid: boolean;
  current: boolean; // true if this is the currently placed piece
  alreadyTried: boolean; // true if this piece was already tested (based on placement strategy)
  invalidReason?: string; // null if valid, otherwise reason
}

export interface NeighborInfo {
  row: number;
  col: number;
  pieceId?: number; // null if neighbor is empty
  rotation?: number; // null if neighbor is empty
  edgeValue?: number; // Required edge value to match neighbor
}

export interface CellConstraints {
  borders: { [direction: string]: boolean }; // "north", "east", "south", "west" -> true if on border
  neighbors: { [direction: string]: NeighborInfo }; // direction -> neighbor info
}

export interface CellPosition {
  row: number;
  col: number;
}

export interface CellStatistics {
  totalPieces: number; // Total pieces in puzzle
  usedPieces: number; // Pieces already placed
  availablePieces: number; // Pieces not yet placed
  validOptions: number; // Number of valid piece+rotation combinations
  invalidOptions: number; // Number of invalid combinations tested
}

export interface CellDetails {
  position: CellPosition;
  currentPiece?: PieceOption; // null if cell is empty
  possiblePieces: PieceOption[];
  constraints: CellConstraints;
  statistics: CellStatistics;
}
