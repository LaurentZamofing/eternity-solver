import React from 'react';
import './PieceVisualizer.css';

export interface PieceVisualizerProps {
  north: number;
  east: number;
  south: number;
  west: number;
  rotation?: number;
  size?: number;
  pieceId?: number;
  showLabel?: boolean;
  className?: string;
  onClick?: () => void;
}

export const PieceVisualizer: React.FC<PieceVisualizerProps> = ({
  north,
  east,
  south,
  west,
  rotation = 0,
  size = 100,
  pieceId,
  showLabel = false,
  className = '',
  onClick
}) => {
  // Use pre-rendered piece images (complete pieces with 45° algorithm applied)
  const getPieceImageUrl = (id: number | undefined): string | null => {
    if (!id || id < 1 || id > 256) return null;
    const paddedId = String(id).padStart(3, '0');
    return `/pieces/${paddedId}.png`;
  };

  const pieceUrl = getPieceImageUrl(pieceId);
  const rotationDeg = rotation * 90;

  return (
    <div
      className={`piece-visualizer ${className}`}
      style={{
        width: `${size}px`,
        height: `${size}px`,
        position: 'relative',
        cursor: onClick ? 'pointer' : 'default',
        backgroundImage: pieceUrl ? `url(${pieceUrl})` : 'none',
        backgroundColor: pieceUrl ? 'transparent' : '#f0f0f0',
        backgroundSize: 'contain',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
        transform: `rotate(${rotationDeg}deg)`,
        transformOrigin: 'center'
      }}
      onClick={onClick}
      title={pieceId ? `Piece ${pieceId}: N=${north} E=${east} S=${south} W=${west} (rot=${rotation})` : 'Empty cell'}
    >
      {showLabel && pieceId !== undefined && (
        <div
          className="piece-label"
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: `translate(-50%, -50%) rotate(-${rotationDeg}deg)`,
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            color: 'white',
            padding: '2px 6px',
            borderRadius: '3px',
            fontSize: '11px',
            fontWeight: 'bold',
            pointerEvents: 'none',
            userSelect: 'none'
          }}
        >
          {pieceId}
        </div>
      )}
    </div>
  );
};

export default PieceVisualizer;
