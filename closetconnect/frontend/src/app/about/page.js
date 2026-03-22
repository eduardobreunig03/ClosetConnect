import Header from "../../components/Header";
import TeamProfile from "../../components/TeamProfile";

export default function AboutPage() {
  return (
    <main className="bg-pink-50 text-rose-900">
      <Header />

      {/* Hero Section */}
      <section className="py-16 px-6 text-center bg-rose-100">
        <h1 className="text-5xl font-serif font-bold mb-4">
          About Closet Connect
        </h1>
        <p className="text-lg max-w-2xl mx-auto">
          Closet Connect is your personal gateway to curated fashion, luxury
          styling, and wardrobe empowerment.
        </p>
      </section>

      {/* Team Intro */}
      <section className="py-16 px-6 bg-rose-50">
        <div className="max-w-5xl mx-auto text-center">
          <h2 className="text-4xl md:text-5xl font-serif font-semibold mb-6">
            Meet the Team
          </h2>
          <p className="text-gray-700 text-lg md:text-xl mb-8">
            Closet Connect is built by a passionate team of designers,
            developers, and fashion lovers who care deeply about helping you
            shine.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-8 justify-items-center">
            <TeamProfile
              name="Ava"
              role="Creative Director"
              imageSrc="/ava.png"
            />
            <TeamProfile
              name="Luna"
              role="Lead Developer"
              imageSrc="/luna.png"
            />
            <TeamProfile
              name="Emily"
              role="Lead Tester"
              imageSrc="/emily.png"
            />
            <TeamProfile
              name="Jasmine"
              role="Lead Doomsayer"
              imageSrc="/jasmine.png"
            />
          </div>
        </div>
      </section>

      {/* Mission Statement */}
      <section className="py-12 px-6 bg-white">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-3xl font-serif font-semibold mb-4">
            Our Mission
          </h2>
          <p className="text-gray-700 text-lg">
            We believe fashion should be expressive, empowering, and effortless.
            Closet Connect helps you discover your style, organize your
            wardrobe, and connect with a community that celebrates
            individuality.
          </p>
        </div>
      </section>

      {/* Call to Action */}
      <section className="py-12 px-6 text-center bg-gradient-to-r from-rose-200 to-pink-300 rounded-xl shadow-md mx-4 my-8">
        <h2 className="text-3xl font-serif font-bold text-white mb-4">
          Ready to Elevate Your Style?
        </h2>
        <p className="text-white text-lg mb-6">
          Join Closet Connect and start your journey toward a more luxurious
          wardrobe experience.
        </p>
        <a
          href="/signup"
          className="bg-white text-rose-600 font-semibold py-3 px-6 rounded-full shadow hover:bg-rose-50 transition"
        >
          Get Started
        </a>
      </section>
    </main>
  );
}
