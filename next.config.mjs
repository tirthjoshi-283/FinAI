/** @type {import('next').NextConfig} */
const nextConfig = {
    // Enable gzip + brotli compression for all responses
    compress: true,

    // Remove X-Powered-By header (minor security + perf)
    poweredByHeader: false,

    // React strict mode helps catch issues early but doubles effects — keep on in dev
    reactStrictMode: false,

    // Experimental: enable concurrent features and package imports optimisation
    experimental: {
        // Tree-shake large icon/component packages so only used exports are bundled
        optimizePackageImports: [
            'lucide-react',
            'recharts',
            'date-fns',
            'framer-motion',
            'firebase',
            'firebase-admin',
        ],
    },

    // SWC modularize imports — secondary tree-shake for lucide-react
    modularizeImports: {
        'lucide-react': {
            transform: 'lucide-react/dist/esm/icons/{{ kebabCase member }}',
            preventFullImport: true,
        },
    },

    images: {
        // Allow external news thumbnail images
        remotePatterns: [
            { protocol: 'https', hostname: '**' },
        ],
        // Use modern AVIF + WebP formats
        formats: ['image/avif', 'image/webp'],
    },
};

export default nextConfig;
