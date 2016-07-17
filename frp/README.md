# Tetris in a Functional Relational Programming style

An attempt to write Tetris in a structure similar to what the Functional Relational Programming paper describes.

FRP paper: http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.93.8928

## Usage

Starting the server:
lein ring server-headless 3000

Starting the browser repl:
lein trampoline cljsbuild repl-launch firefox http://localhost:3000/

Point your broswer to http://localhost:3000

## Demo

http://bmaddy.github.com/tetris/

## License

Distributed under the Eclipse Public License, the same as Clojure.
