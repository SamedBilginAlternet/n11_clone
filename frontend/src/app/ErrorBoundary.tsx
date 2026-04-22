import { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // eslint-disable-next-line no-console
    console.error('ErrorBoundary caught', error, info);
  }

  render() {
    if (this.state.error) {
      return (
        this.props.fallback ?? (
          <div className="max-w-xl mx-auto mt-20 bg-white rounded-lg border border-red-200 p-6 text-center">
            <div className="text-4xl mb-2">⚠️</div>
            <h1 className="text-xl font-bold mb-2">Bir şeyler ters gitti</h1>
            <p className="text-sm text-gray-600 mb-4">{this.state.error.message}</p>
            <button
              className="bg-n11-purple text-white px-4 py-2 rounded font-medium hover:bg-purple-700"
              onClick={() => window.location.reload()}
            >
              Sayfayı yenile
            </button>
          </div>
        )
      );
    }
    return this.props.children;
  }
}
