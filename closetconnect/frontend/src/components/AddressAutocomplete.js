"use client";

import { useRef } from "react";
import { useJsApiLoader, Autocomplete } from "@react-google-maps/api";

export default function AddressAutocomplete({ value, onChange }) {
  const autocompleteRef = useRef(null);

  const { isLoaded, loadError } = useJsApiLoader({
    googleMapsApiKey: process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY,
    libraries: ["places"], // needed for Autocomplete
  });

  if (loadError) return <p>Failed to load Google Maps</p>;
  if (!isLoaded) return <p>Loading...</p>;

  return (
    <Autocomplete
      onLoad={(autocomplete) => (autocompleteRef.current = autocomplete)}
      onPlaceChanged={() => {
        const place = autocompleteRef.current.getPlace();
        if (place?.formatted_address) onChange(place.formatted_address);
        else if (place?.name) onChange(place.name);
      }}
    >
      <input
        type="text"
        placeholder="Start typing an address..."
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-xl border px-3 py-2"
      />
    </Autocomplete>
  );
}
