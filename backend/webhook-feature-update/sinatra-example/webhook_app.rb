

require 'sinatra'
require 'json'

set :bind, '0.0.0.0'

post "/" do
  raw_data = request.body.read
  puts "raw data is #{raw_data}"
  data = JSON.parse(raw_data)
  puts "headers are:"
  puts request.env['HTTP_CE_SUBJECT']
  puts request.env['HTTP_CE_ID']
  puts request.env['HTTP_CE_SPECVERSION']
  puts request.env['HTTP_CE_SOURCE']
  puts request.env['HTTP_CE_TIME']
  'yay!'
end
