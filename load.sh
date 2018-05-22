#!/usr/bin/env bash

d=$HOME/Desktop
i=$d/in
o=$d/out
rm -rf $i/*
echo $i $o $d


function ctrl_c(){
  rm -rf $i/*
  rm -rf $o/*
  echo "goodbye.."
  exit
}

trap ctrl_c INT

while true; do
  rnd=$RANDOM
  fn=${rnd}.txt
  echo "writing $rnd to ${rnd}.txt.."
  echo ${rnd}  > $i/$fn
  sleep 10
done
