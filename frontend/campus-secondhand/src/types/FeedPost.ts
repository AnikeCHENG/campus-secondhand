export type PostType = 'sell' | 'buy' | 'chat' | 'free' | 'warning'

export interface FeedPost {
  id: number | string
  userId: number | string
  user: string
  avatar?: string
  time: string
  location?: string
  type: PostType
  typeLabel: string
  typeIcon?: string
  content: string
  images: string[]
  price: number | null
  status?: 'selling' | 'sold' | 'wanted' | 'resolved'
  tags: string[]
  likes: number
  comments: number
  shares?: number
}
