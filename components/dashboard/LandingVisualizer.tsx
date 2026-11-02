"use client";

import { useRef, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { Environment, Float } from '@react-three/drei';
import * as THREE from 'three';

function InnerCore() {
    const meshRef = useRef<THREE.Mesh>(null!);

    useFrame((state, delta) => {
        if (meshRef.current) {
            meshRef.current.rotation.x -= delta * 0.2;
            meshRef.current.rotation.y += delta * 0.3;
        }
    });

    const material = useMemo(() => new THREE.MeshPhysicalMaterial({
        color: new THREE.Color("#10b981"),
        emissive: new THREE.Color("#00422b"),
        emissiveIntensity: 0.8,
        wireframe: false,
        transmission: 0.9,
        ior: 1.5,
        roughness: 0
    }), []);

    return (
        <mesh ref={meshRef} material={material}>
            <sphereGeometry args={[1.2, 32, 32]} />
        </mesh>
    );
}

function WireframeIcosahedron() {
    const meshRef = useRef<THREE.Mesh>(null!);

    useFrame((state, delta) => {
        if (meshRef.current) {
            meshRef.current.rotation.y += delta * 0.15;
            meshRef.current.rotation.z += delta * 0.05;
        }
    });

    const material = useMemo(() => new THREE.MeshBasicMaterial({
        color: new THREE.Color("#6ffbbe"),
        wireframe: true,
        transparent: true,
        opacity: 0.2
    }), []);

    return (
        <mesh ref={meshRef} material={material}>
            <icosahedronGeometry args={[2.5, 1]} />
        </mesh>
    );
}

export function LandingVisualizer() {
    return (
        <div className="w-full h-[500px] lg:h-[600px] relative">
            <Canvas camera={{ position: [0, 0, 6], fov: 45 }}>
                <ambientLight intensity={0.5} />
                <pointLight position={[10, 10, 10]} intensity={1.5} color="#10b981" />
                <pointLight position={[-10, -10, -10]} intensity={0.5} color="#00422b" />
                
                <Float speed={2} rotationIntensity={0.5} floatIntensity={1}>
                    <InnerCore />
                    <WireframeIcosahedron />
                </Float>
                
                <Environment preset="city" />
            </Canvas>
        </div>
    );
}
