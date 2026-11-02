"use client";

import { useRef, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { Float, Environment, PerspectiveCamera, Torus } from '@react-three/drei';
import * as THREE from 'three';

function RotatingRings() {
    const groupRef = useRef<THREE.Group>(null!);

    useFrame((state, delta) => {
        if (groupRef.current) {
            groupRef.current.rotation.x += delta * 0.2;
            groupRef.current.rotation.y += delta * 0.5;
        }
    });

    const material1 = useMemo(() => new THREE.MeshPhysicalMaterial({
        color: new THREE.Color("#10b981"),
        emissive: new THREE.Color("#00422b"),
        emissiveIntensity: 0.5,
        clearcoat: 1.0,
        roughness: 0.2,
        metalness: 0.8,
    }), []);

    const material2 = useMemo(() => new THREE.MeshPhysicalMaterial({
        color: new THREE.Color("#34d399"),
        emissive: new THREE.Color("#059669"),
        emissiveIntensity: 0.8,
        clearcoat: 1.0,
        roughness: 0.1,
        metalness: 1.0,
        wireframe: true,
    }), []);

    return (
        <group ref={groupRef}>
            {/* Outer Target Ring */}
            <Torus args={[1.5, 0.15, 16, 100]} material={material1} />
            {/* Inner Wireframe Ring rotating oppositely */}
            <Torus args={[1.0, 0.2, 16, 50]} material={material2} rotation={[Math.PI / 2, 0, 0]}>
                <meshBasicMaterial color="#a7f3d0" wireframe />
            </Torus>
            {/* Bullseye Core */}
            <mesh>
                <sphereGeometry args={[0.4, 32, 32]} />
                <meshStandardMaterial color="#00ff88" emissive="#00ff88" emissiveIntensity={1} />
            </mesh>
        </group>
    );
}

export function GoalVisualizer() {
    return (
        <div className="w-full h-full min-h-[250px] relative pointer-events-auto">
            <Canvas className="w-full h-full">
                <PerspectiveCamera makeDefault position={[0, 0, 5]} fov={50} />
                <ambientLight intensity={0.5} />
                <pointLight position={[10, 10, 10]} intensity={1.2} color="#10b981" />
                
                <Float speed={3} rotationIntensity={0.6} floatIntensity={1.5}>
                    <RotatingRings />
                </Float>
                
                <Environment preset="city" />
            </Canvas>
        </div>
    );
}
