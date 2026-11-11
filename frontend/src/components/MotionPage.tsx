import { motion, useReducedMotion } from 'motion/react'
import type { ReactNode } from 'react'

export function MotionPage({ children }: { children: ReactNode }) {
  const reduceMotion = useReducedMotion()

  return (
    <motion.main
      animate={{ opacity: 1, y: 0 }}
      className="page-motion"
      initial={{ opacity: 0, y: reduceMotion ? 0 : 10 }}
      transition={{ duration: reduceMotion ? 0 : 0.28, ease: 'easeOut' }}
    >
      {children}
    </motion.main>
  )
}
