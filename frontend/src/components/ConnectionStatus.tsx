interface ConnectionStatusProps {
  connected: boolean;
}

function ConnectionStatus({ connected }: ConnectionStatusProps) {
  return (
    <div className="flex items-center space-x-2">
      <div
        className={`w-3 h-3 rounded-full ${
          connected ? 'bg-green-500 animate-pulse' : 'bg-red-500'
        }`}
      ></div>
      <span className="text-sm text-gray-600 dark:text-gray-400">
        {connected ? 'Connected' : 'Disconnected'}
      </span>
    </div>
  );
}

export default ConnectionStatus;
