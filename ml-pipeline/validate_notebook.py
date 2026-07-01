import json

nb = json.load(open('ml-pipeline/cairn_training.ipynb', encoding='utf-8'))

# Validate Colab requirements
errors = []

if nb['nbformat'] != 4:
    errors.append('Bad nbformat')
if 'colab' not in nb['metadata']:
    errors.append('No colab metadata')
if nb['metadata'].get('accelerator') != 'GPU':
    errors.append('No GPU accelerator set')

for i, cell in enumerate(nb['cells']):
    if 'cell_type' not in cell:
        errors.append('Cell %d: no cell_type' % i)
    if 'source' not in cell:
        errors.append('Cell %d: no source' % i)
    elif not isinstance(cell['source'], list):
        errors.append('Cell %d: source must be list, got %s' % (i, type(cell['source'])))
    if cell.get('cell_type') == 'code':
        if 'execution_count' not in cell:
            errors.append('Cell %d: no execution_count' % i)
        if 'outputs' not in cell:
            errors.append('Cell %d: no outputs' % i)
        elif not isinstance(cell['outputs'], list):
            errors.append('Cell %d: outputs must be list' % (i,))
    if 'metadata' not in cell:
        errors.append('Cell %d: no metadata' % i)

if errors:
    print('ERRORS:')
    for e in errors:
        print('  ' + e)
else:
    print('All Colab compatibility checks PASSED')
    print('Cells: %d' % len(nb['cells']))
    for i, c in enumerate(nb['cells']):
        t = c['cell_type']
        src = ''.join(c['source'])
        first = src.split('\n')[0][:60]
        print('  [%d] %s: %s' % (i, t, first))
