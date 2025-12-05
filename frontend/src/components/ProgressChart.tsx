import { useEffect, useRef } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ChartOptions,
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import type { HistoricalMetrics } from '../types/metrics';

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

interface ProgressChartProps {
  history: HistoricalMetrics[];
}

function ProgressChart({ history }: ProgressChartProps) {
  const chartRef = useRef(null);

  // Prepare chart data
  const labels = history.map((item) => {
    const date = new Date(item.timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  });

  const depthData = history.map((item) => item.depth);
  const progressData = history.map((item) => item.progressPercentage);

  const data = {
    labels,
    datasets: [
      {
        label: 'Depth',
        data: depthData,
        borderColor: 'rgb(59, 130, 246)',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        yAxisID: 'y',
        tension: 0.1,
      },
      {
        label: 'Progress %',
        data: progressData,
        borderColor: 'rgb(16, 185, 129)',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        yAxisID: 'y1',
        tension: 0.1,
      },
    ],
  };

  const options: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index' as const,
      intersect: false,
    },
    plugins: {
      legend: {
        position: 'top' as const,
        labels: {
          color: 'rgb(156, 163, 175)',
        },
      },
      title: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: function (context) {
            let label = context.dataset.label || '';
            if (label) {
              label += ': ';
            }
            if (context.parsed.y !== null) {
              if (context.dataset.label === 'Progress %') {
                label += context.parsed.y.toFixed(2) + '%';
              } else {
                label += context.parsed.y;
              }
            }
            return label;
          },
        },
      },
    },
    scales: {
      x: {
        ticks: {
          color: 'rgb(156, 163, 175)',
          maxRotation: 45,
          minRotation: 45,
        },
        grid: {
          color: 'rgba(156, 163, 175, 0.1)',
        },
      },
      y: {
        type: 'linear' as const,
        display: true,
        position: 'left' as const,
        title: {
          display: true,
          text: 'Depth',
          color: 'rgb(59, 130, 246)',
        },
        ticks: {
          color: 'rgb(156, 163, 175)',
        },
        grid: {
          color: 'rgba(156, 163, 175, 0.1)',
        },
      },
      y1: {
        type: 'linear' as const,
        display: true,
        position: 'right' as const,
        title: {
          display: true,
          text: 'Progress %',
          color: 'rgb(16, 185, 129)',
        },
        ticks: {
          color: 'rgb(156, 163, 175)',
        },
        grid: {
          drawOnChartArea: false,
        },
      },
    },
  };

  return (
    <div className="h-64 md:h-96">
      <Line ref={chartRef} data={data} options={options} />
    </div>
  );
}

export default ProgressChart;
