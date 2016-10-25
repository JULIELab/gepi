grep 'Performance' slurm-163.out | awk '{sum+=$17}END{print sum}'
grep 'Sending batch' slurm-163.out | awk '{sum+=$11}END{print sum}'
