"use client";
import {useEffect,useRef,type ReactNode} from 'react';
export function ScrollReveal({children,className=''}:{children:ReactNode;className?:string}){
 const ref=useRef<HTMLDivElement>(null);
 useEffect(()=>{const el=ref.current;if(!el||window.matchMedia('(prefers-reduced-motion: reduce)').matches||!('IntersectionObserver' in window))return;if(el.getBoundingClientRect().top<window.innerHeight){el.classList.add('is-visible');return;}el.classList.add('reveal-ready');const observer=new IntersectionObserver(entries=>{if(entries[0].isIntersecting){el.classList.add('is-visible');observer.disconnect();}},{threshold:.08});observer.observe(el);return()=>observer.disconnect();},[]);
 return <div ref={ref} className={`scroll-reveal ${className}`}>{children}</div>;
}
