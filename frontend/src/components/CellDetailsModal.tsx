import React, { useState, useEffect } from 'react';
import type { CellDetails, PieceOption } from '../types/metrics';
import { PieceVisualizer } from './PieceVisualizer';

interface CellDetailsModalProps {
  configName: string;
  row: number;
  col: number;
  onClose: () => void;
}

export const CellDetailsModal: React.FC<CellDetailsModalProps> = ({
  configName,
  row,
  col,
  onClose,
}) => {
  const [cellDetails, setCellDetails] = useState<CellDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterMode, setFilterMode] = useState<'all' | 'valid' | 'tried'>('all');
  const [viewMode, setViewMode] = useState<'current' | 'historical'>('current');

  useEffect(() => {
    const loadCellDetails = async () => {
      try {
        setLoading(true);
        setError(null);

        // Build URL based on view mode
        const baseUrl = `http://localhost:8080/api/configs/${configName}/cell/${row}/${col}/details`;
        const url = viewMode === 'historical' ? `${baseUrl}/historical` : baseUrl;

        const response = await fetch(url);

        if (!response.ok) {
          throw new Error(`Failed to load cell details: ${response.statusText}`);
        }

        const data = await response.json();
        setCellDetails(data);
      } catch (err) {
        console.error('Error loading cell details:', err);
        setError(err instanceof Error ? err.message : 'Unknown error');
      } finally {
        setLoading(false);
      }
    };

    loadCellDetails();
  }, [configName, row, col, viewMode]);

  // Filter pieces based on mode
  const filteredPieces = cellDetails?.possiblePieces.filter(piece => {
    if (filterMode === 'valid') {
      return piece.valid;
    } else if (filterMode === 'tried') {
      return piece.alreadyTried || piece.current;
    }
    return true;
  }) || [];

  // Group pieces by status for display
  const currentPiece = cellDetails?.currentPiece;
  const validPieces = filteredPieces.filter(p => p.valid && !p.current);
  const invalidPieces = filteredPieces.filter(p => !p.valid && !p.current);
  const triedPieces = filteredPieces.filter(p => p.alreadyTried && !p.current);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-[95vw] w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
              Cell Details
            </h2>
            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
              {configName} - Position [{row}, {col}]
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {loading && (
            <div className="text-center py-8">
              <p className="text-gray-600 dark:text-gray-400">Loading cell details...</p>
            </div>
          )}

          {error && (
            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
              <p className="text-red-800 dark:text-red-200">Error: {error}</p>
            </div>
          )}

          {cellDetails && (
            <>
              {/* Statistics */}
              <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
                  <p className="text-sm text-gray-600 dark:text-gray-400">Total Pieces</p>
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">
                    {cellDetails.statistics.totalPieces}
                  </p>
                </div>
                <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
                  <p className="text-sm text-gray-600 dark:text-gray-400">Used</p>
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">
                    {cellDetails.statistics.usedPieces}
                  </p>
                </div>
                <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
                  <p className="text-sm text-gray-600 dark:text-gray-400">Available</p>
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">
                    {cellDetails.statistics.availablePieces}
                  </p>
                </div>
                <div className="bg-green-50 dark:bg-green-900/20 p-4 rounded-lg border border-green-200 dark:border-green-800">
                  <p className="text-sm text-green-600 dark:text-green-400">Valid Options</p>
                  <p className="text-2xl font-bold text-green-900 dark:text-green-100">
                    {cellDetails.statistics.validOptions}
                  </p>
                </div>
                <div className="bg-red-50 dark:bg-red-900/20 p-4 rounded-lg border border-red-200 dark:border-red-800">
                  <p className="text-sm text-red-600 dark:text-red-400">Invalid</p>
                  <p className="text-2xl font-bold text-red-900 dark:text-red-100">
                    {cellDetails.statistics.invalidOptions}
                  </p>
                </div>
              </div>

              {/* Constraints */}
              <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-3">
                  Constraints
                </h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="font-medium text-gray-700 dark:text-gray-300">Borders:</p>
                    <ul className="ml-4 mt-1 space-y-1 text-gray-600 dark:text-gray-400">
                      {Object.entries(cellDetails.constraints.borders).map(([dir, isBorder]) => (
                        isBorder && <li key={dir}>{dir}</li>
                      ))}
                      {Object.values(cellDetails.constraints.borders).every(v => !v) && (
                        <li>No borders</li>
                      )}
                    </ul>
                  </div>
                  <div>
                    <p className="font-medium text-gray-700 dark:text-gray-300">Neighbors:</p>
                    <ul className="ml-4 mt-1 space-y-1 text-gray-600 dark:text-gray-400">
                      {Object.entries(cellDetails.constraints.neighbors).map(([dir, neighbor]) => (
                        <li key={dir}>
                          {dir}: Piece {neighbor.pieceId} r{neighbor.rotation} (edge: {neighbor.edgeValue})
                        </li>
                      ))}
                      {Object.keys(cellDetails.constraints.neighbors).length === 0 && (
                        <li>No neighbors</li>
                      )}
                    </ul>
                  </div>
                </div>
              </div>

              {/* View Mode Toggle */}
              <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
                <h3 className="text-sm font-semibold text-blue-900 dark:text-blue-100 mb-2">
                  View Mode
                </h3>
                <div className="flex space-x-2">
                  <button
                    onClick={() => setViewMode('current')}
                    className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                      viewMode === 'current'
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
                    }`}
                  >
                    Current Constraints
                  </button>
                  <button
                    onClick={() => setViewMode('historical')}
                    className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                      viewMode === 'historical'
                        ? 'bg-purple-600 text-white'
                        : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
                    }`}
                  >
                    Historical (At Time of Placement)
                  </button>
                </div>
                <p className="text-xs text-blue-800 dark:text-blue-200 mt-2">
                  {viewMode === 'current'
                    ? 'Showing constraints with all currently placed pieces'
                    : 'Showing constraints as they were when this piece was placed (excludes pieces placed later)'}
                </p>
              </div>

              {/* Filter Buttons */}
              <div className="flex space-x-2">
                <button
                  onClick={() => setFilterMode('all')}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                    filterMode === 'all'
                      ? 'bg-blue-500 text-white'
                      : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
                  }`}
                >
                  All ({cellDetails.possiblePieces.length})
                </button>
                <button
                  onClick={() => setFilterMode('valid')}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                    filterMode === 'valid'
                      ? 'bg-green-500 text-white'
                      : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
                  }`}
                >
                  Valid ({cellDetails.statistics.validOptions})
                </button>
                <button
                  onClick={() => setFilterMode('tried')}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                    filterMode === 'tried'
                      ? 'bg-yellow-500 text-white'
                      : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
                  }`}
                >
                  Already Tried ({triedPieces.length})
                </button>
              </div>

              {/* Current Piece */}
              {currentPiece && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-3">
                    Current Piece
                  </h3>
                  <div className="flex items-center space-x-4">
                    <PieceVisualizer
                      pieceId={currentPiece.pieceId}
                      rotation={currentPiece.rotation}
                      north={currentPiece.edges[0]}
                      east={currentPiece.edges[1]}
                      south={currentPiece.edges[2]}
                      west={currentPiece.edges[3]}
                      size={80}
                      showLabel={true}
                    />
                    <div className="text-sm text-gray-600 dark:text-gray-400">
                      <p>Piece ID: {currentPiece.pieceId}</p>
                      <p>Rotation: {currentPiece.rotation} (×90°)</p>
                      <p>Edges: [{currentPiece.edges.join(', ')}]</p>
                    </div>
                  </div>
                </div>
              )}

              {/* Possible Pieces */}
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-3">
                  Possible Pieces
                  <span className="ml-2 text-sm font-normal text-gray-600 dark:text-gray-400">
                    ({filteredPieces.length} options)
                  </span>
                </h3>
                <div className="grid grid-cols-4 sm:grid-cols-6 md:grid-cols-8 lg:grid-cols-10 gap-3">
                  {filteredPieces.map((piece, index) => (
                    <div key={`${piece.pieceId}-${piece.rotation}-${index}`} className="text-center">
                      <PieceVisualizer
                        pieceId={piece.pieceId}
                        rotation={piece.rotation}
                        north={piece.edges[0]}
                        east={piece.edges[1]}
                        south={piece.edges[2]}
                        west={piece.edges[3]}
                        size={60}
                        showLabel={false}
                      />
                      <p className="text-xs text-gray-600 dark:text-gray-400 mt-1">
                        {piece.pieceId} r{piece.rotation}
                      </p>
                    </div>
                  ))}
                </div>
                {filteredPieces.length === 0 && (
                  <p className="text-center text-gray-500 dark:text-gray-400 py-8">
                    No pieces match the current filter
                  </p>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
