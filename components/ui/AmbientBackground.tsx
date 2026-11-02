"use client";

import { CSSParticles } from './CSSParticles';

export function AmbientBackground() {
  return (
    <div className="fixed inset-0 z-0 pointer-events-none overflow-hidden">

      {/* Subtle star field */}
      <div
        className="absolute inset-0 opacity-[0.18]"
        style={{
          backgroundImage: 'radial-gradient(circle at center, rgba(255,255,255,0.9) 0, rgba(255,255,255,0) 1px)',
          backgroundSize: '80px 80px',
        }}
      />

      {/* Aurora Glow Orbs — Violet */}
      <div
        className="absolute top-[-15%] left-[-8%] w-[55%] h-[55%] rounded-full blur-[130px] pointer-events-none"
        style={{
          background: 'radial-gradient(circle, rgba(124,58,237,0.22) 0%, transparent 70%)',
          animation: 'auroraShift 18s ease-in-out infinite',
          willChange: 'transform',
        }}
      />
      {/* Aurora Glow Orbs — Cyan */}
      <div
        className="absolute top-[20%] right-[-12%] w-[50%] h-[60%] rounded-full blur-[140px] pointer-events-none"
        style={{
          background: 'radial-gradient(circle, rgba(34,211,238,0.14) 0%, transparent 70%)',
          animation: 'auroraShift 22s ease-in-out infinite reverse',
          animationDelay: '3s',
          willChange: 'transform',
        }}
      />
      {/* Bottom Violet accent */}
      <div
        className="absolute bottom-[-10%] left-[15%] w-[70%] h-[40%] rounded-full blur-[110px] pointer-events-none"
        style={{
          background: 'radial-gradient(circle, rgba(99,38,200,0.16) 0%, transparent 70%)',
          animation: 'auroraShift 25s ease-in-out infinite',
          animationDelay: '8s',
          willChange: 'transform',
        }}
      />

      {/* Animated SVG Node Network */}
      <svg
        className="absolute inset-0 w-full h-full"
        viewBox="0 0 1440 900"
        preserveAspectRatio="xMidYMid slice"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        {/* Connecting lines — animated stroke draw */}
        <line x1="200" y1="150" x2="500" y2="320" stroke="rgba(124,58,237,0.3)" strokeWidth="1"
          strokeDasharray="400" style={{ animation: 'drawLine 6s ease-in-out infinite', animationDelay: '0s' }} />
        <line x1="500" y1="320" x2="820" y2="200" stroke="rgba(34,211,238,0.25)" strokeWidth="1"
          strokeDasharray="400" style={{ animation: 'drawLine 7s ease-in-out infinite', animationDelay: '1s' }} />
        <line x1="820" y1="200" x2="1150" y2="380" stroke="rgba(124,58,237,0.25)" strokeWidth="1"
          strokeDasharray="400" style={{ animation: 'drawLine 8s ease-in-out infinite', animationDelay: '0.5s' }} />
        <line x1="1150" y1="380" x2="1350" y2="180" stroke="rgba(34,211,238,0.2)" strokeWidth="1"
          strokeDasharray="400" style={{ animation: 'drawLine 6s ease-in-out infinite', animationDelay: '2s' }} />
        <line x1="500" y1="320" x2="680" y2="560" stroke="rgba(124,58,237,0.2)" strokeWidth="1"
          strokeDasharray="400" style={{ animation: 'drawLine 9s ease-in-out infinite', animationDelay: '1.5s' }} />
        <line x1="680" y1="560" x2="1000" y2="650" stroke="rgba(34,211,238,0.2)" strokeWidth="1"
          strokeDasharray="400" style={{ animation: 'drawLine 7s ease-in-out infinite', animationDelay: '3s' }} />
        <line x1="200" y1="150" x2="100" y2="420" stroke="rgba(124,58,237,0.18)" strokeWidth="1"
          strokeDasharray="300" style={{ animation: 'drawLine 10s ease-in-out infinite', animationDelay: '0.8s' }} />
        <line x1="820" y1="200" x2="680" y2="560" stroke="rgba(159,103,255,0.18)" strokeWidth="1"
          strokeDasharray="400" style={{ animation: 'drawLine 11s ease-in-out infinite', animationDelay: '2.5s' }} />

        {/* Nodes — circles with float animation */}
        {[
          { cx: 200, cy: 150, r: 5, color: '#7C3AED', delay: '0s' },
          { cx: 500, cy: 320, r: 7, color: '#9F67FF', delay: '0.6s' },
          { cx: 820, cy: 200, r: 6, color: '#22D3EE', delay: '1.2s' },
          { cx: 1150, cy: 380, r: 5, color: '#7C3AED', delay: '0.3s' },
          { cx: 1350, cy: 180, r: 4, color: '#22D3EE', delay: '1.8s' },
          { cx: 680, cy: 560, r: 6, color: '#9F67FF', delay: '0.9s' },
          { cx: 1000, cy: 650, r: 5, color: '#22D3EE', delay: '1.5s' },
          { cx: 100, cy: 420, r: 4, color: '#7C3AED', delay: '2.1s' },
        ].map((node, i) => (
          <g key={i} style={{ animation: `floatNode 5s ease-in-out infinite`, animationDelay: node.delay }}>
            {/* Outer glow ring */}
            <circle cx={node.cx} cy={node.cy} r={node.r + 8} fill={node.color} opacity="0.08" />
            {/* Core dot */}
            <circle cx={node.cx} cy={node.cy} r={node.r} fill={node.color} opacity="0.7" />
            {/* Inner bright center */}
            <circle cx={node.cx} cy={node.cy} r={node.r * 0.45} fill="white" opacity="0.6" />
          </g>
        ))}

        {/* Flowing energy wave paths */}
        <path
          d="M0 500 Q360 380 720 500 Q1080 620 1440 480"
          stroke="rgba(124,58,237,0.12)" strokeWidth="1.5" fill="none"
          strokeDasharray="800"
          style={{ animation: 'drawLine 12s ease-in-out infinite', animationDelay: '0s' }}
        />
        <path
          d="M0 600 Q400 480 800 580 Q1100 660 1440 560"
          stroke="rgba(34,211,238,0.10)" strokeWidth="1" fill="none"
          strokeDasharray="800"
          style={{ animation: 'drawLine 15s ease-in-out infinite', animationDelay: '2s' }}
        />
      </svg>

      {/* Subtle dot-grid overlay */}
      <div
        className="absolute inset-0 opacity-[0.04]"
        style={{
          backgroundImage: `
            radial-gradient(circle, rgba(255,255,255,1) 1px, transparent 1px)
          `,
          backgroundSize: '40px 40px',
        }}
      />

      {/* CSS Particles — recolored violet */}
      <CSSParticles
        count={25}
        color="rgba(124, 58, 237, 0.30)"
        speed="slow"
        direction="up"
      />
    </div>
  );
}

