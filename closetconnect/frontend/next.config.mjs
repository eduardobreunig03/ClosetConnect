/** @type {import('next').NextConfig} */
const nextConfig = {
  async redirects() {
    return [
      {
        source: '/',
        destination: '/home',
        permanent: false,
      },
    ];
  },
  async rewrites() {
    return [{ source: "/api/:path*", destination: "http://localhost:8080/api/:path*" }];
  },
  experimental: {
    allowedDevOrigins: ["http://localhost:3000", "http://10.16.149.66:3000"],
  },
  turbopack: {
    root: './', // 👈 set this to your real frontend folder if needed
  },
};

export default nextConfig;
