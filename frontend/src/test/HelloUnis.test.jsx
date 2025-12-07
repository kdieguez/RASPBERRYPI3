import React from 'react'
import { render, screen } from '@testing-library/react'
import { HelloUnis } from '../components/HelloUnis'

describe('HelloUnis', () => {
  it('muestra Hola UNIS por defecto', () => {
    render(<HelloUnis />)
    expect(screen.getByText('Hola UNIS')).toBeInTheDocument()
  })

  it('muestra el nombre personalizado', () => {
    render(<HelloUnis name="UNIS" />)
    expect(screen.getByText('Hola UNIS')).toBeInTheDocument()
  })
})
