

require 'sinatra'
require 'json'

set :bind, '0.0.0.0'

post "/" do
  raw_data = request.body.read
  puts "\n\nraw data is #{raw_data}"
  puts "headers are:"
  request.env.select {|k,v| k.start_with? 'HTTP_'}
         .collect {|key, val| [key.sub(/^HTTP_/, ''), val]}
         .collect {|key, val| "#{key}: #{val}"}
         .sort
         .each { |h| puts "#{h}" }
  'yay!'
end
