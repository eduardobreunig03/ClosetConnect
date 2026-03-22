"use client";

import { GoogleMap, Marker, useJsApiLoader } from "@react-google-maps/api";

const defaultCenter = {
  lat: -33.8688, // Sydney
  lng: 151.2093,
};

const containerStyle = {
  width: "100%",
  height: "400px",
};

export default function Map({ center = defaultCenter, zoom = 12 }) {
  const { isLoaded, loadError } = useJsApiLoader({
    googleMapsApiKey: process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY,
    libraries: ["places"], // add this line
  });

  if (loadError) {
    console.error("Error loading Google Maps:", loadError);
    return <p>Map failed to load</p>;
  }

  if (!isLoaded) return <p>Loading map...</p>;

  return (
    <div className="w-full h-[400px] rounded-2xl overflow-hidden shadow-md border border-gray-200">
      <GoogleMap
        mapContainerStyle={containerStyle}
        center={center}
        zoom={zoom}
        options={{
          disableDefaultUI: true,
          zoomControl: true,
          mapTypeControl: false,
          streetViewControl: false,
          fullscreenControl: false,
          styles: [
            {
              featureType: "poi",
              stylers: [{ visibility: "off" }],
            },
          ],
        }}
      >
        <Marker position={center} />
      </GoogleMap>
    </div>
  );
}
