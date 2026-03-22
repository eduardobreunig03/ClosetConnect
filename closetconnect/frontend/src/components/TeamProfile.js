// components/TeamProfile.jsx
export default function TeamProfile({ name, role, imageSrc }) {
  return (
    <div className="bg-white p-6 rounded-lg shadow w-56 md:w-64">
      <img
        src={imageSrc}
        alt={name}
        className="h-32 w-32 rounded-full mx-auto mb-4 object-cover"
      />
      <h3 className="text-xl font-semibold">{name}</h3>
      <p className="text-md text-gray-600">{role}</p>
    </div>
  );
}
