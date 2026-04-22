import { forwardRef, InputHTMLAttributes } from 'react';

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  hint?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, Props>(function Input(
  { label, hint, error, className = '', id, ...rest },
  ref,
) {
  const inputId = id ?? rest.name;
  return (
    <label className="block" htmlFor={inputId}>
      {label && <span className="text-sm font-medium text-gray-700">{label}</span>}
      <input
        ref={ref}
        id={inputId}
        {...rest}
        className={`mt-1 w-full rounded border px-3 py-2 focus:outline-none focus:ring-2 ${
          error
            ? 'border-red-400 focus:ring-red-300'
            : 'border-gray-300 focus:ring-n11-purple'
        } ${className}`}
      />
      {error ? (
        <p className="text-red-600 text-xs mt-1">{error}</p>
      ) : hint ? (
        <p className="text-xs text-gray-500 mt-1">{hint}</p>
      ) : null}
    </label>
  );
});
