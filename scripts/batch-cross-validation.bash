#!/bin/bash -l
# The -l above is required to get the full environment with modules

# Set the allocation to be charged for this job
# not required if you have set a default allocation
#SBATCH -A 2017-130

# The name of the script
#SBATCH -J tangrams-cv

# Wall-clock time that will be given to this job
#SBATCH -t 24:00:00

# Number of nodes
#SBATCH --nodes=2
# Number of MPI processes per node
#SBATCH --ntasks-per-node=24

PROJECT_DIR="/cfs/klemming/nobackup/${LOGNAME:0:1}/${LOGNAME}/tangrams-restricted"
#PROJECT_DIR="${HOME}/Private/tangrams-restricted"

#SBATCH -e "${PROJECT_DIR}/output/sbatch.err.txt"
#SBATCH -o "${PROJECT_DIR}/output/sbatch.err.txt"

# Load the Intel MPI module
module add intelmpi/17.0.1
module add jdk/1.8.0_45

CLASSPATH_JARFILE="${PROJECT_DIR}/tangrams-wac-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
echo "JAR file is \"${CLASSPATH_JARFILE}\"."
INPATH="${PROJECT_DIR}/Data/Ready"
echo "Inpath is \"${INPATH}\"."
REFLANG_FILE="${PROJECT_DIR}/Data/utt-referring-tokens-lemma.tsv"
echo "Referring-language file is \"${REFLANG_FILE}\"."
PARAMS_FILE="${PROJECT_DIR}/Data/model-params.tsv"
echo "Model parameter file is \"${PARAMS_FILE}\"."
OUTDIR="${PROJECT_DIR}/output/tangrams-wac"
echo "Outdir is \"${OUTDIR}\"."

STD_OUTFILE="${PROJECT_DIR}/output/tangrams-wac.out.txt"
echo "Standard output stream is directed to \"${STD_OUTFILE}\"."
ERR_OUTFILE="${PROJECT_DIR}/output/tangrams-wac.err.txt"
echo "Error output stream is directed to \"${ERR_OUTFILE}\"."

HEAP_SIZE="10240m"
echo "Will use heap space size of ${HEAP_SIZE}."

# Run the executable with MPI-rank of 48
mpirun -n 48 --bind-to none java -server -Xmx${HEAP_SIZE} -jar "${CLASSPATH_JARFILE}" "${INPATH}" -t "${REFLANG_FILE}" -p "${PARAMS_FILE}" -o "${OUTDIR}" > "${STD_OUTFILE}" 2> "${ERR_OUTFILE}"