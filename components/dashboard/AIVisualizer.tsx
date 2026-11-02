"use client";

import { useRef, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { Float, Points, PointMaterial } from '@react-three/drei';
import * as THREE from 'three';

// Generate random points in a sphere shell
function generateNeuralPoints(count: number, radius: number) {
    const points = new Float32Array(count * 3);
    for (let i = 0; i < count; i++) {
        const theta = Math.random() * 2 * Math.PI;
        const phi = Math.acos(Math.random() * 2 - 1);
        const r = radius + (Math.random() - 0.5) * 0.5; // slight variance
        points[i * 3] = r * Math.sin(phi) * Math.cos(theta);
        points[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
        points[i * 3 + 2] = r * Math.cos(phi);
    }
    return points;
}

function NeuralSwarm() {
    const ref = useRef<THREE.Points>(null!);
    const points = useMemo(() => generateNeuralPoints(600, 2), []);
    
    useFrame((state, delta) => {
        if (ref.current) {
            ref.current.rotation.x -= delta / 10;
            ref.current.rotation.y -= delta / 15;
            
            // Heartbeat scale pulse
            const pulse = 1 + Math.sin(state.clock.elapsedTime * 2) * 0.05;
            ref.current.scale.set(pulse, pulse, pulse);
        }
    });

    return (
        <Points ref={ref} positions={points} stride={3}>
            <PointMaterial
                transparent
                color="#10b981"
                size={0.08}
                sizeAttenuation={true}
                depthWrite={false}
                opacity={0.8}
            />
        </Points>
    );
}

function CentralCore() {
    const meshRef = useRef<THREE.Mesh>(null!);

    useFrame((state, delta) => {
        if (meshRef.current) {
            meshRef.current.rotation.y += delta * 0.5;
        }
    });

    const material = useMemo(() => new THREE.MeshPhysicalMaterial({
        color: new THREE.Color("#00422b"),
        emissive: new THREE.Color("#10b981"),
        emissiveIntensity: 0.5,
        wireframe: true,
    }), []);

    return (
        <mesh ref={meshRef} material={material}>
            <icosahedronGeometry args={[0.8, 2]} />
        </mesh>
    );
}

export function AIVisualizer() {
    return (
        <div className="w-full h-full min-h-[250px] relative">
            <Canvas camera={{ position: [0, 0, 5], fov: 60 }}>
                <ambientLight intensity={0.5} />
                <Float speed={2} rotationIntensity={0.2} floatIntensity={0.5}>
                    <CentralCore />
                    <NeuralSwarm />
                </Float>
            </Canvas>
        </div>
    );
}
