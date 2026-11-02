"use client";

import { useRef, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { Float, Environment, PerspectiveCamera, ContactShadows } from '@react-three/drei';
import * as THREE from 'three';

function GlowingCrystal() {
    const meshRef = useRef<THREE.Mesh>(null!);

    // Soft rotation
    useFrame((state, delta) => {
        if (meshRef.current) {
            meshRef.current.rotation.y += delta * 0.4;
            meshRef.current.rotation.z += delta * 0.1;
        }
    });

    // We'll create a stylized octahedron to represent balancing wealth
    const material = useMemo(() => new THREE.MeshPhysicalMaterial({
        color: new THREE.Color("#10b981"), // Emerald Primary
        emissive: new THREE.Color("#00422b"),
        emissiveIntensity: 0.5,
        roughness: 0.1,
        metalness: 0.8,
        transmission: 0.9, // glass-like
        ior: 1.5,
        thickness: 2.0,
        clearcoat: 1.0,
        clearcoatRoughness: 0.1,
    }), []);

    return (
        <Float speed={2.5} rotationIntensity={0.5} floatIntensity={1.5}>
            <mesh ref={meshRef} material={material} castShadow receiveShadow>
                <octahedronGeometry args={[1.5, 0]} />
            </mesh>
            
            {/* Core glowing sphere inside the crystal */}
            <mesh>
                <sphereGeometry args={[0.6, 32, 32]} />
                <meshBasicMaterial color="#6ffbbe" transparent opacity={0.6} />
            </mesh>
        </Float>
    );
}

export function NetWorthVisualizer() {
    return (
        <div className="w-full h-full min-h-[300px] relative pointer-events-auto">
            {/* Ambient Background Glow matching the model */}
            <div className="absolute inset-0 bg-primary/10 rounded-full blur-[80px] pointer-events-none transform scale-75 -z-10" />
            
            <Canvas shadows className="w-full h-full">
                <PerspectiveCamera makeDefault position={[0, 0, 5]} fov={50} />
                <ambientLight intensity={0.4} />
                <spotLight position={[10, 10, 10]} angle={0.15} penumbra={1} intensity={1} castShadow />
                <pointLight position={[-10, -10, -10]} intensity={0.5} color="#006c49" />
                
                <GlowingCrystal />
                
                <Environment preset="city" />
                <ContactShadows position={[0, -2, 0]} opacity={0.4} scale={10} blur={2} far={4} color="#00422b" />
            </Canvas>
        </div>
    );
}
